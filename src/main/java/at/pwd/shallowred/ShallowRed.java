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

import java.util.ArrayList;
import java.util.List;
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

    private static final String DEFAULT_CONFIG = "{  \"C\":0.7071067811865475,  \"minmaxInfluence\":0,  \"selector\":{\"type\":\"roulette\"},  \"expand\": {    \"0\": {      \"weight\": 0.5    },    \"1\": {      \"weight\": 1    },    \"2\": {      \"weight\": 0.05    },    \"3\": {      \"weight\": 0.05    },    \"4\": {      \"weight\": 0.05    },    \"5\": {      \"weight\": 0.75    }  },  \"simulation\":{    \"0\": {      \"weight\": 0.5    },    \"1\": {      \"weight\": 1    },    \"2\": {      \"weight\": 0.05    },    \"3\": {      \"weight\": 0.05    },    \"4\": {      \"weight\": 0.05    },    \"5\": {      \"weight\": 0.75    }  }, \"useEndgameDB\": true}";

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
        private int visitCount;
        private int winCount;

        private MancalaGame game;
        private ShallowRed.MCTSTree parent;
        private List<ShallowRed.MCTSTree> children = new ArrayList<>(6);
        int actionId;
        //calculated using minmax and differences of stones in depot, the bigger the better for player 0
        double minMaxValue;

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
            ShallowRed.MCTSTree best = null;
            double value = 0;
            for (ShallowRed.MCTSTree m : children) {
                double wC = (double)m.winCount;
                double vC = (double)m.visitCount;
                double currentValue =  (1-minmaxInfluence)*wC/vC + //mcts exploitation term
                                        minmaxInfluence*(game.getCurrentPlayer()-m.minMaxValue)*-MINMAX_PLAYER_MULT[game.getCurrentPlayer()] + //minMaxValue if PLAYER_A, 1-minMaxValue if PLAYER_B
                                        C*Math.sqrt(2*Math.log(visitCount) / vC);//Exploration term


                if (best == null || currentValue > value) {
                    value = currentValue;
                    best = m;
                }
            }

            return best;
        }

        int getBestMove()
        {
            int best = 0;
            double value = 0;
            for (ShallowRed.MCTSTree m : children) {
                double wC = (double)m.winCount;
                double vC = (double)m.visitCount;
                double currentValue =  (1-minmaxInfluence)*wC/vC + //mcts exploitation term
                        minmaxInfluence*(game.getCurrentPlayer()-m.minMaxValue)*-MINMAX_PLAYER_MULT[game.getCurrentPlayer()]; //minMaxValue if PLAYER_A, 1-minMaxValue if PLAYER_B


                if (best == 0 || currentValue > value) {
                    value = currentValue;
                    best = m.actionId;
                }
            }

            return best;
        }

        boolean isFullyExpanded() {
            return children.size() == game.getSelectableCount();
        }

        ShallowRed.MCTSTree move(int actionId) {
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
                tree.minMaxValue=newGame.getWinner()==MancalaBoard.PLAYER_A?PROVEN_GAME_VALUE:-PROVEN_GAME_VALUE;
            }
            else if(useEndgameDB && EndgameDB.hasValueStored(newGame))
            {
                //calculate if this game is a win using the difference of stones stored in the database
                int aDepot = newGame.getBoard().getFields()[MancalaBoard.DEPOT_A];
                int bDepot = newGame.getBoard().getFields()[MancalaBoard.DEPOT_B];
                int dbValue = EndgameDB.loadDBValue(newGame)*MINMAX_PLAYER_MULT[newGame.getCurrentPlayer()];
                tree.minMaxValue = aDepot-bDepot+dbValue>0?PROVEN_GAME_VALUE:-PROVEN_GAME_VALUE;
            }
            else
            {
                //calculate heuristic value using difference of stones
                int aDepot = newGame.getBoard().getFields()[MancalaBoard.DEPOT_A];
                int bDepot = newGame.getBoard().getFields()[MancalaBoard.DEPOT_B];
                tree.minMaxValue = (aDepot - bDepot + (aDepot + bDepot)) / (double) (2 * (aDepot + bDepot));
            }

            this.children.add(tree);

            updateMinMaxValue();

            return tree;
        }

        void updateMinMaxValue()
        {
            if(children.isEmpty())
                return;

            int playerMult = MINMAX_PLAYER_MULT[game.getCurrentPlayer()];
            minMaxValue = children.get(0).minMaxValue;
            for(int i = 1;i<children.size();++i)
            {
                MCTSTree child = children.get(i);
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
                for (int i = 0; i < children.size(); ++i)
                {
                    MCTSTree result = children.get(i).searchRoot(searchGame);
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
                //clear parent
                root.parent = null;
                root.actionId = 0;
            }

        }
    }

    @Override
    public MancalaAgentAction doTurn(int computationTime, at.pwd.boardgame.game.mancala.MancalaGame game) {

        //create mapping list between our own implementation and the existing one
        if(mancalaMapping == null){
            mancalaMapping = new String[14];//index is the ID of a slot from the mancalaboard, the content is the String ID from the existing implementation

            String slotID = game.getBoard().getDepotOfPlayer(0);
            mancalaMapping[0] = slotID;

            for(int i = 13; i >= 1;i--){
                slotID = game.getBoard().next(slotID);
                mancalaMapping[i] = slotID;
            }

            playerId = game.getState().getCurrentPlayer();
        }

        long start = System.currentTimeMillis();

        //convert given mancala game to our MancalaGame class
        MancalaGame convertedGame = new MancalaGame(game);

        //search root of monte carlo tree, convert MancalaGame from framework to our Mancala game
        reuseSubtree(convertedGame);

        while ((System.currentTimeMillis() - start) < (computationTime*1000 - 100)) {
            ShallowRed.MCTSTree best = treePolicy(root);
            int winner = defaultPolicy(best.game);
            backup(best, winner);
        }

        //get best move
        int selected = root.getBestMove();
        //System.out.println("Selected action " + selected.winCount + " / " + selected.visitCount);
        /*System.out.println("Selected action: "+selected);

        for(MCTSTree child : root.children)
        {
            System.out.println(child.actionId+": "+child.winCount + " / " + child.visitCount+" | Avg: "+child.winCount/(float)child.visitCount+" |  MinMaxValue: "+child.minMaxValue);
        }
        System.out.println("Total Visit count: "+root.visitCount);*/

        return new MancalaAgentAction(mancalaMapping[MancalaBoard.index(game.getState().getCurrentPlayer(),selected)]);
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
        //filter already expanded moves
        for(int i = 1;i<=6;++i)
            expandSelectionFilter[i]=true;
        for(MCTSTree child : best.children)
            expandSelectionFilter[child.actionId] = false;

        return best.move(selector.select(best.game,expandHeuristics,expandWeights, expandSelectionFilter));
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
