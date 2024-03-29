package at.pwd.shallowred;

import at.pwd.boardgame.game.mancala.agent.MancalaAgent;
import at.pwd.boardgame.game.mancala.agent.MancalaAgentAction;
import at.pwd.shallowred.CustomGame.MancalaBoard;
import at.pwd.shallowred.CustomGame.MancalaGame;
import at.pwd.shallowred.CustomGame.MancalaGamePool;
import at.pwd.shallowred.EndgameDB.EndgameDB;
import at.pwd.shallowred.Heuristics.Heuristic;
import at.pwd.shallowred.Heuristics.HeuristicSettings;
import at.pwd.shallowred.Utils.Pool;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;

import java.util.Random;

public class ShallowRed implements MancalaAgent {

    private MCTSTree root;
    private MancalaGamePool gamePool = new MancalaGamePool();
    private MCTSTreePool nodePool = new MCTSTreePool();
    private Random r = new Random();
    private double C;

    private static final int[] MINMAX_PLAYER_MULT = new int[]{1,-1};

    //this is used as minimax value when a proven win can be reached
    private static final float PROVEN_GAME_VALUE = 10000000;

    private static final boolean[] SIMULATION_SELECTION_FILTER = new boolean[]{true,true,true,true,true,true,true};
    private final boolean[] expandSelectionFilter = new boolean[7];

    //heuristic configuration
    private int[] expandHeuristicIds;
    private int[] simulationHeuristicIds;
    private Heuristic[] expandHeuristics;
    private Heuristic[] simulationHeuristics;
    private float[] expandWeights;
    private float[] simulationWeights;
    private SelectionUtils selector;

    //the exploitation term of UCT consists of minmaxInfluence*minmaxValue+(1-minmaxInfluence)*win/visited
    private double minmaxInfluence;

    private String[] mancalaMapping;
    //id of this agent
    private int playerId;

    private boolean useEndgameDB = false;

    private static final String DEFAULT_CONFIG = "{  \"C\":0.5,  \"minmaxInfluence\":0.975,  \"selector\":{\"type\":\"random\"},  \"expand\": {},  \"simulation\":{}, \"useEndgameDB\": false}";

    public ShallowRed()
    {
        this(DEFAULT_CONFIG);
    }

    public ShallowRed(String jsonConfig)
    {
        //load endgame db
        EndgameDB.loadDB();

        selector = new SelectionUtils();
        loadJSONConfig(jsonConfig);
    }

    public class MCTSTreePool extends Pool<MCTSTree>
    {
        @Override
        protected MCTSTree instantiate()
        {
            return new MCTSTree();
        }
    }

    private class MCTSTree {
        private MancalaGame game;
        private ShallowRed.MCTSTree parent;
        private ShallowRed.MCTSTree[] children = new ShallowRed.MCTSTree[6];

        //calculated using minmax and differences of stones in depot, the bigger the better for player 0,
        //is set to PROVEN_GAME_VALUE if this node is a guaranteed win, -PROVEN_GAME_VALUE on a proven loss otherwise [0,1]
        private double minMaxValue;
        private int visitCount;
        private int winCount;

        private byte actionId;
        private byte childrenCount = 0;


        MCTSTree()
        {
        }

        MCTSTree set(MancalaGame game)
        {
            this.game = game;
            return this;
        }

        boolean isNonTerminal() {
            return game.getWinner()==MancalaGame.NOBODY;
        }

        ShallowRed.MCTSTree getBestNode() {
            ShallowRed.MCTSTree best = children[0];
            double value = 0;
            for(int i = 0;i<childrenCount;++i) {
                ShallowRed.MCTSTree m  = children[i];
                //ignore nodes where the result is already set
                if(m.isProven())
                    continue;

                double wC = (double)m.winCount;
                double vC = (double)m.visitCount;
                double currentValue =  (1-minmaxInfluence)*wC/vC + //mcts exploitation term
                                        minmaxInfluence*(game.getCurrentPlayer()-m.minMaxValue)*-MINMAX_PLAYER_MULT[game.getCurrentPlayer()] + //minMaxValue if PLAYER_A, 1-minMaxValue if PLAYER_B
                                        C*Math.sqrt(2*Math.log(visitCount) / vC);//Exploration term


                if (currentValue > value) {
                    value = currentValue;
                    best = m;
                }
            }

            return best;
        }

        ShallowRed.MCTSTree getBestMove()
        {
            ShallowRed.MCTSTree best = null;
            double value = 0;
            for(int i = 0;i<childrenCount;++i) {
                ShallowRed.MCTSTree m  = children[i];
                double wC = (double)m.winCount;
                double vC = (double)m.visitCount;
                double currentValue =  (1-minmaxInfluence)*wC/vC + //mcts exploitation term
                        minmaxInfluence*(game.getCurrentPlayer()-m.minMaxValue)*-MINMAX_PLAYER_MULT[game.getCurrentPlayer()]; //minMaxValue if PLAYER_A, 1-minMaxValue if PLAYER_B


                if (best == null || currentValue > value) {
                    value = currentValue;
                    best = m;
                }
            }

            return best;
        }

        /**
         * @return true if this node is a proven loss or win by the endgamedb or terminal node
         */
        boolean isProven()
        {
            return minMaxValue<0 || minMaxValue>1;
        }

        boolean isFullyExpanded() {
            return childrenCount == game.getSelectableCount();
        }

        ShallowRed.MCTSTree move(byte actionId) {
            //obtain a copy
            MancalaGame newGame = gamePool.obtain().copy(this.game);
            //perform turn
            newGame.performTurn(actionId);

            ShallowRed.MCTSTree tree = nodePool.obtain().set(newGame);
            tree.actionId = actionId;
            tree.parent = this;

            //calculate initial value for minmaxValue
            if(newGame.getWinner()!=MancalaGame.NOBODY)
            {
                //terminal node reached look who is the winner
                if(newGame.getWinner()==MancalaBoard.PLAYER_A)
                    tree.minMaxValue = PROVEN_GAME_VALUE;
                else if(newGame.getWinner()==MancalaBoard.PLAYER_B)
                    tree.minMaxValue = -PROVEN_GAME_VALUE;
                else
                    tree.minMaxValue = 0.5;
            }
            else if(useEndgameDB && EndgameDB.hasValueStored(newGame))
            {
                //calculate if this game is a win using the difference of stones stored in the database
                int aDepot = newGame.getBoard().getFields()[MancalaBoard.DEPOT_A];
                int bDepot = newGame.getBoard().getFields()[MancalaBoard.DEPOT_B];
                int dbValue = EndgameDB.loadDBValue(newGame)*MINMAX_PLAYER_MULT[newGame.getCurrentPlayer()];
                int value = aDepot-bDepot+dbValue;
                if(value==0)
                    tree.minMaxValue = 0.5;
                else
                    tree.minMaxValue = value>0?PROVEN_GAME_VALUE:-PROVEN_GAME_VALUE;
            }
            else
            {
                //calculate heuristic value using difference of stones
                int aDepot = newGame.getBoard().getFields()[MancalaBoard.DEPOT_A];
                int bDepot = newGame.getBoard().getFields()[MancalaBoard.DEPOT_B];
                tree.minMaxValue = (aDepot - bDepot + (aDepot + bDepot)) / (double) (2 * (aDepot + bDepot));
            }

            addChild(tree);

            updateMinMaxValue();

            return tree;
        }

        void addChild(ShallowRed.MCTSTree child)
        {
            //add new child
            this.children[childrenCount] = child;
            ++childrenCount;
        }

        void clearChildren()
        {
            for(int i = 0;i<childrenCount;++i)
                children[i] = null;
            childrenCount = 0;
        }

        void updateMinMaxValue()
        {
            if(childrenCount==0)
                return;

            int playerMult = MINMAX_PLAYER_MULT[game.getCurrentPlayer()];
            minMaxValue = children[0].minMaxValue;
            for(int i = 1;i<childrenCount;++i)
            {
                MCTSTree child = children[i];
                minMaxValue = playerMult * Math.max(playerMult * minMaxValue, playerMult * child.minMaxValue);
            }
        }

        MCTSTree searchRoot(MancalaGame searchGame)
        {
            if(game.getBoard().equals(searchGame.getBoard()))
                return this;

            //if enemy did last turn and now it's my turn, check equality
            if(parent!=null && parent.game.getCurrentPlayer()!=searchGame.getCurrentPlayer() && game.getCurrentPlayer()!=searchGame.getCurrentPlayer())
            {
                return null;
            }
            else
            {
                //go deeper, check games of children
                for (int i = 0; i < childrenCount; ++i)
                {
                    MCTSTree result = children[i].searchRoot(searchGame);
                    if(result!=null)
                        return result;
                }

                return null;//game was not in this subtree
            }
        }
    }

    private void reuseSubtree(MancalaGame game)
    {
        //if there is no root, create a new node
        if(root==null)
            root = nodePool.obtain().set(game);
        else
        {
            root = root.searchRoot(game);

            //if not found (=path was not visited once) create new node
            if(root==null)
                root = nodePool.obtain().set(game);
            else
            {
                //detatch from rest of the tree, so GC can clean up
                if(root.parent!=null)
                {
                    //remove children from parent
                    root.clearChildren();
                    //clear parent
                    root.parent = null;

                }

                root.actionId = 0;
            }

        }
    }

    @Override
    public MancalaAgentAction doTurn(int computationTime, at.pwd.boardgame.game.mancala.MancalaGame game) {

        long start = System.currentTimeMillis();

        //create mapping list between our own implementation and the existing one
        if(mancalaMapping == null){
            mancalaMapping = new String[14];//index is the ID of a slot from the mancalaboard, the content is the String ID from the existing implementation

            String slotID = game.getBoard().getDepotOfPlayer(0);
            mancalaMapping[0] = slotID;

            for(int i = 13; i >= 1;i--){
                slotID = game.getBoard().next(slotID);
                mancalaMapping[i] = slotID;
            }

        }
        playerId = game.getState().getCurrentPlayer();

        //convert given mancala game to our MancalaGame class
        MancalaGame convertedGame = new MancalaGame(game);

        //search root of monte carlo tree, convert MancalaGame from framework to our Mancala game
        reuseSubtree(convertedGame);

        while ((System.currentTimeMillis() - start) < (computationTime*1000 - 100)) {
            ShallowRed.MCTSTree best = treePolicy(root);
            int winner = -1;
            if(!best.isProven())
                winner = defaultPolicy(best.game);
            backup(best, winner);
        }

        //get best move
        root = root.getBestMove();

        //detatch parts of the tree for gc
        root.clearChildren();
        root.parent = null;

        return new MancalaAgentAction(mancalaMapping[MancalaBoard.index(game.getState().getCurrentPlayer(),root.actionId)]);
    }

    private void backup(ShallowRed.MCTSTree current, int winner) {
        int hasWon = (winner==playerId)?1:0;

        while (current != null) {
            // always increase visit count
            current.visitCount++;

            // if it ended in a win => increase the win count
            current.winCount += hasWon;

            //update minmax value
            current.updateMinMaxValue();

            current = current.parent;
        }
    }

    private ShallowRed.MCTSTree treePolicy(ShallowRed.MCTSTree current) {
        while (current.isNonTerminal()) {
            if (!current.isFullyExpanded()) {
                return expand(current);
            } else {
                current = current.getBestNode();
            }
        }
        return current;
    }

    private ShallowRed.MCTSTree expand(ShallowRed.MCTSTree best) {
        if(best.isProven() && best!=root)
            return best;

        //filter already expanded moves
        for(int i = 1;i<=6;++i)
            expandSelectionFilter[i]=true;
        for(int i = 0;i<best.childrenCount;++i)
        {
            MCTSTree child = best.children[i];
            expandSelectionFilter[child.actionId] = false;
        }

        return best.move((byte)selector.select(best.game,expandHeuristics,expandWeights, expandSelectionFilter));
    }

    private int defaultPolicy(MancalaGame game) {
        game = gamePool.obtain().copy(game); // copy original game

        int turnId;
        while(game.getWinner()==MancalaGame.NOBODY)
        {
            turnId = selector.select(game,simulationHeuristics,simulationWeights,SIMULATION_SELECTION_FILTER);
            game.performTurn(turnId);
        }

        //free game used for playthrough
        gamePool.free(game);

        return game.getWinner();
    }

    @Override
    public String toString() {
        return "ShallowRed";
    }

    public int[] getExpandHeuristics()
    {
        return expandHeuristicIds;
    }

    public float[] getExpandWeights()
    {
        return expandWeights;
    }

    /**
     * Preconditions:
     *      @param expandHeuristicIds !=null, must only contain valid ids
     *      @param expandWeights !=null, every element must be in [-1,1]
     */
    public void setExpandHeuristics(int[] expandHeuristicIds, float[] expandWeights)
    {
        this.expandHeuristicIds = expandHeuristicIds;
        this.expandWeights = expandWeights;

        //reload heuristics
        expandHeuristics = HeuristicSettings.generateHeuristicArray(expandHeuristicIds);
    }

    public int[] getSimulationHeuristics()
    {
        return simulationHeuristicIds;
    }

    public float[] getSimulationWeights()
    {
        return simulationWeights;
    }

    /**
     * Preconditions:
     *      @param simulationHeuristicIds !=null, must only contain valid ids
     *      @param simulationWeights !=null, every element must be in [-1,1]
     */
    public void setSimulationHeuristics(int[] simulationHeuristicIds, float[] simulationWeights)
    {
        this.simulationHeuristicIds = simulationHeuristicIds;
        this.simulationWeights = simulationWeights;

        //reload heuristics
        simulationHeuristics = HeuristicSettings.generateHeuristicArray(simulationHeuristicIds);
    }

    /*======================================
        JSON Serialization
     =======================================*/
    /**
     *
     * @param json !=null
     * @return true if configuration could successfully be loaded, false otherwise
     */
    public boolean loadJSONConfig(String json)
    {
        JsonObject root = Json.parse(json).asObject();

        //load C
        C = root.getDouble("C",1.0/Math.sqrt(2.0));

        //load minmax influence
        minmaxInfluence = root.get("minmaxInfluence").asDouble();

        //load selector
        selector.setSelectionAlg(root.get("selector").asObject());

        //load expand heuristics & weights
        JsonObject exp = root.get("expand").asObject();
        expandHeuristicIds = new int[exp.size()];
        expandWeights = new float[expandHeuristicIds.length];
        loadHeuristics(exp, expandHeuristicIds,expandWeights);

        expandHeuristics = HeuristicSettings.generateHeuristicArray(expandHeuristicIds);

        //load simulation heuristics & weights
        JsonObject sim = root.get("simulation").asObject();
        simulationHeuristicIds = new int[sim.size()];
        simulationWeights = new float[simulationHeuristicIds.length];
        loadHeuristics(sim, simulationHeuristicIds,simulationWeights);

        //load if endgamedb should be used
        useEndgameDB = root.getBoolean("useEndgameDB",false);

        simulationHeuristics = HeuristicSettings.generateHeuristicArray(simulationHeuristicIds);

        return true;
    }

    private void loadHeuristics(JsonObject hConfig, int[] heuristicsOut, float[] weightsOut)
    {
        int x = 0;
        for(JsonObject.Member member : hConfig)
        {
            heuristicsOut[x] = Integer.parseInt(member.getName());
            JsonObject heu = member.getValue().asObject();
            weightsOut[x] = heu.get("weight").asFloat();

            ++x;
        }
    }

    public String toJSONConfig()
    {
        JsonObject exp = Json.object();
        JsonObject sim = Json.object();

        fillHeuristicsJson(exp,expandHeuristicIds,expandWeights);
        fillHeuristicsJson(sim,simulationHeuristicIds,simulationWeights);

        return Json.object().
                add("C",C).
                add("minmaxInfluence",minmaxInfluence).
                add("selector",selector.getSelectionAlg().toJSON()).
                add("expand",exp).
                add("simulation",sim).
                add("useEndgameDB",useEndgameDB).
                toString(WriterConfig.PRETTY_PRINT);

    }

    private void fillHeuristicsJson(JsonObject hConfig,int[] heuristics, float[] weights)
    {
        for(int x = 0;x<heuristics.length;++x)
        {
            JsonObject heu = Json.object();
            heu.add("weight",weights[x]);
            hConfig.add(Integer.toString(heuristics[x]),heu);
        }
    }
}
