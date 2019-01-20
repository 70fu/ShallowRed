package at.pwd.shallowred.Heuristics;

//this class is not used anymore since the ShallowRed agent can be configured using json files
public class HeuristicSettings
{
    private HeuristicSettings(){}

    /*=============================================
        Array of all Heuristics & Heuristic-Ids
     ============================================*/
    //array of all heuristics
    private static final Heuristic[] heuristics =
            {
                    HeuristicFunctions::extraTurn,
                    HeuristicFunctions::stealStones,
                    HeuristicFunctions::moveOverDepot,
                    HeuristicFunctions::extraTurnChaining,
                    HeuristicFunctions::stealOpportunity,
                    HeuristicFunctions::preventStealLight
            };

    //index numbers (=ID) of all heuristics in the heuristics array
    public static final int EXTRA_TURN = 0;
    public static final int STEAL_STONES = 1;
    public static final int MOVE_OVER_DEPOT = 2;
    public static final int EXTRA_TURN_CHAINING = 3;
    public static final int STEAL_OPPORTUNITY = 4;
    public static final int PREVENT_STEAL_LIGHT = 5;

    /*==============================================
        SETTINGS
    ================================================*/
    private static final int[] EXPAND_HEURISTIC_SETTINGS=
            {
                    EXTRA_TURN
            };
    private static final int[] SIMULATION_HEURISTIC_SETTINGS=EXPAND_HEURISTIC_SETTINGS;



    /*=============================================
        Heuristic-array generation from settings
     ==============================================*/

    public static final Heuristic[] EXPAND_HEURISTICS;
    public static final Heuristic[] SIMULATION_HEURISTICS;

    static
    {
        EXPAND_HEURISTICS = generateHeuristicArray(EXPAND_HEURISTIC_SETTINGS);
        SIMULATION_HEURISTICS = generateHeuristicArray(SIMULATION_HEURISTIC_SETTINGS);
    }

    /**
     * Preconditions:
     *      @param heuristicIds !=null, for each element i: i>=0 && i<=heuristics.length
     * Postconditions:
     *      @return a subset of the heuristics array, where the id of each element was given in the parameter heuristicIds
     */
    public static Heuristic[] generateHeuristicArray(int[] heuristicIds)
    {
        Heuristic[] h = new Heuristic[heuristicIds.length];
        for(int x = 0;x<heuristicIds.length;++x)
            h[x] = getHeuristic(heuristicIds[x]);
        return h;
    }

    /**
     * Preconditions:
     *      @param heuristicId >=0 && <=heuristics.length
     * Postconditions:
     *      @return heuristic with given id
     */
    public static Heuristic getHeuristic(int heuristicId)
    {
        return heuristics[heuristicId];
    }
}
