package at.pwd.shallowred;

import at.pwd.boardgame.game.mancala.agent.MancalaAgent;
import at.pwd.boardgame.game.mancala.agent.MancalaAgentAction;
import at.pwd.shallowred.CustomGame.MancalaBoard;
import at.pwd.shallowred.CustomGame.MancalaGame;
import at.pwd.shallowred.CustomGame.MancalaGamePool;
import at.pwd.shallowred.Heuristics.Heuristic;
import at.pwd.shallowred.Heuristics.HeuristicSettings;
import at.pwd.shallowred.Utils.Pool;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * TODO Keep Subtree of Search tree for next turn
 */
public class ShallowRed implements MancalaAgent {

    private MancalaGamePool gamePool = new MancalaGamePool();
    private MCTSTreePool nodePool = new MCTSTreePool();
    private Random r = new Random();
    private double C;

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

    private String[] mancalaMapping;
    //id of this agent
    private int playerId;

    private static final String DEFAULT_CONFIG = "{  \"C\":0.7071067811865475,\"selector\":{\"type\":\"roulette\"},  \"expand\":[    {      \"id\":0,      \"weight\":0.5    },    {      \"id\":1,      \"weight\":1    },    {      \"id\":2,      \"weight\":0.05    },{      \"id\":3,      \"weight\":0.05    },{      \"id\":4,      \"weight\":0.05    },{      \"id\":5,      \"weight\":0.5    }  ],  \"simulation\":[    {      \"id\":0,      \"weight\":0.5    },    {      \"id\":1,      \"weight\":1    },    {      \"id\":2,      \"weight\":0.05    },{      \"id\":3,      \"weight\":0.05    },{      \"id\":4,      \"weight\":0.05    },{      \"id\":5,      \"weight\":0.5    }  ]}";

    public ShallowRed()
    {
        this(DEFAULT_CONFIG);
    }

    public ShallowRed(String jsonConfig)
    {
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
                double currentValue =  wC/vC + C*Math.sqrt(2*Math.log(visitCount) / vC);


                if (best == null || currentValue > value) {
                    value = currentValue;
                    best = m;
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

            this.children.add(tree);

            return tree;
        }
    }

    @Override
    public MancalaAgentAction doTurn(int computationTime, at.pwd.boardgame.game.mancala.MancalaGame game) {

        //create mapping list between our own implementation and the existing one
        if(mancalaMapping == null){
            int numFields = 14;
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

        //create root of monte carlo tree, convert MancalaGame from framework to our Mancala game
        ShallowRed.MCTSTree root = nodePool.obtain().set(new MancalaGame(game));

        while ((System.currentTimeMillis() - start) < (computationTime*1000 - 100)) {
            ShallowRed.MCTSTree best = treePolicy(root);
            int winner = defaultPolicy(best.game);
            backup(best, winner);
        }

        ShallowRed.MCTSTree selected = root.getBestNode();
        //System.out.println("Selected action " + selected.winCount + " / " + selected.visitCount);
        //System.out.println("Selected action: "+selected.actionId);

        return new MancalaAgentAction(mancalaMapping[MancalaBoard.index(game.getState().getCurrentPlayer(),selected.actionId)]);
    }

    private void backup(ShallowRed.MCTSTree current, int winner) {
        int hasWon = (winner==playerId)?1:0;

        while (current != null) {
            // always increase visit count
            current.visitCount++;

            // if it ended in a win => increase the win count
            current.winCount += hasWon;

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

        return best.move(selector.select(best.game,expandHeuristics,expandWeights, expandSelectionFilter));//TODO subtract already expanded nodes
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

        //load selector
        selector.setSelectionAlg(root.get("selector").asObject());

        //load expand heuristics & weights
        JsonArray exp = root.get("expand").asArray();
        expandHeuristicIds = new int[exp.size()];
        expandWeights = new float[expandHeuristicIds.length];
        loadHeuristics(exp, expandHeuristicIds,expandWeights);

        expandHeuristics = HeuristicSettings.generateHeuristicArray(expandHeuristicIds);

        //load simulation heuristics & weights
        JsonArray sim = root.get("simulation").asArray();
        simulationHeuristicIds = new int[sim.size()];
        simulationWeights = new float[simulationHeuristicIds.length];
        loadHeuristics(sim, simulationHeuristicIds,simulationWeights);

        simulationHeuristics = HeuristicSettings.generateHeuristicArray(simulationHeuristicIds);

        return true;
    }

    private void loadHeuristics(JsonArray hConfig, int[] heuristicsOut, float[] weightsOut)
    {
        for(int x = 0;x<hConfig.size();++x)
        {
            JsonObject heu = hConfig.get(x).asObject();
            heuristicsOut[x] = heu.get("id").asInt();
            weightsOut[x] = heu.get("weight").asFloat();
        }
    }

    public String toJSONConfig()
    {
        JsonArray exp = Json.array();
        JsonArray sim = Json.array();

        fillHeuristicsJson(exp,expandHeuristicIds,expandWeights);
        fillHeuristicsJson(sim,simulationHeuristicIds,simulationWeights);

        return Json.object().
                add("C",C).
                add("selector",selector.getSelectionAlg().toJSON()).
                add("expand",exp).
                add("simulation",sim).toString(WriterConfig.PRETTY_PRINT);

    }

    private void fillHeuristicsJson(JsonArray hConfig,int[] heuristics, float[] weights)
    {
        for(int x = 0;x<heuristics.length;++x)
        {
            JsonObject heu = Json.object();
            heu.add("id",heuristics[x]);
            heu.add("weight",weights[x]);
            hConfig.add(heu);
        }
    }
}
