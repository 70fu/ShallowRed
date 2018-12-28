package at.pwd.shallowred.tests;

import at.pwd.shallowred.CustomGame.MancalaBoard;
import at.pwd.shallowred.CustomGame.MancalaGame;
import at.pwd.shallowred.Heuristics.Heuristic;
import at.pwd.shallowred.Heuristics.HeuristicFunctions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class HeuristicFunctionsTest
{

    @ParameterizedTest
    @MethodSource("heuristicArguments")
    void heuristicTest(MancalaGame game, Heuristic heuristic, float[] expectedWeights)
    {
        //create array with possible turns
        boolean[] possibleTurns = new boolean[7];
        for(int id=1;id<=6;++id)
            possibleTurns[id] = game.isSelectable(id);

        //init weights array with invalid values
        float[] weights = new float[]{2,2,2,2,2,2,2};

        //evaluate
        heuristic.evaluate(game,possibleTurns,weights);

        //set impossible turns to same value
        for(int id = 0;id<=6;++id)
        {
            if (!possibleTurns[id])
            {
                weights[id] = expectedWeights[id] = -1;
            }
        }

        //compare with expected
        assertArrayEquals(expectedWeights,weights);
    }

    private static Stream<Arguments> heuristicArguments()
    {
        return Stream.of(
                //EXTRA TURN
                Arguments.of(new MancalaGame(MancalaBoard.PLAYER_A,new int[]{0,1,4,3,17,12,0,0,0,0,0,0,0,0}),
                        new HWrapper(HeuristicFunctions::extraTurn),
                        new float[]{0,1,0,1,1,0,0}),
                //same as above for the other player
                Arguments.of(new MancalaGame(MancalaBoard.PLAYER_B,new int[]{0,0,0,0,0,0,0,0,1,4,3,17,12,0}),
                        new HWrapper(HeuristicFunctions::extraTurn),
                        new float[]{0,1,0,1,1,0,0}),

                //STEAL STONES
                Arguments.of(new MancalaGame(MancalaBoard.PLAYER_A,new int[]{0,8,0,0,13,2,0,0,2,2,4,3,2,0}),
                        new HWrapper(HeuristicFunctions::stealStones),
                        new float[]{0,2f/9f,0,0,4f/9f,3/9f,0}),
                //same as above for the other player
                Arguments.of(new MancalaGame(MancalaBoard.PLAYER_B,new int[]{0,2,2,4,3,2,0,0,8,0,0,13,2,0}),
                        new HWrapper(HeuristicFunctions::stealStones),
                        new float[]{0,2f/9f,0,0,4f/9f,3/9f,0}),

                //MOVE OVER DEPOT
                Arguments.of(new MancalaGame(MancalaBoard.PLAYER_A,new int[]{0,2,2,2,30,6,0,1,0,0,0,0,0,0}),
                        new HWrapper(HeuristicFunctions::moveOverDepot),
                        new float[]{0,1,0,0,1,1,0}),
                //same as above for the other player
                Arguments.of(new MancalaGame(MancalaBoard.PLAYER_B,new int[]{0,0,0,0,0,0,0,0,2,2,2,30,6,0}),
                        new HWrapper(HeuristicFunctions::moveOverDepot),
                        new float[]{0,1,0,0,1,1,0}),

                //EXTRA TURN CHAINING
                Arguments.of(new MancalaGame(MancalaBoard.PLAYER_A,new int[]{0,4,2,3,17,12,0,0,0,0,0,0,0,0}),
                        new HWrapper(HeuristicFunctions::extraTurnChaining),
                        new float[]{0,0,1,0,0,0,0}),
                //same as above for the other player
                Arguments.of(new MancalaGame(MancalaBoard.PLAYER_B,new int[]{0,0,0,0,0,0,0,0,4,2,3,17,12,0}),
                        new HWrapper(HeuristicFunctions::extraTurnChaining),
                        new float[]{0,0,1,0,0,0,0}),

                //STEAL OPPORTUNITY
                Arguments.of(new MancalaGame(MancalaBoard.PLAYER_A,new int[]{0,1,1,1,0,1,1,0,1,2,3,4,5,6}),
                        new HWrapper(HeuristicFunctions::stealOpportunity),
                        new float[]{0,6/18f,5/18f,4/18f,0,2/18f,1/18f}),
                //same as above for the other player
                Arguments.of(new MancalaGame(MancalaBoard.PLAYER_B,new int[]{0,1,2,3,4,5,6,0,1,1,1,0,1,1}),
                        new HWrapper(HeuristicFunctions::stealOpportunity),
                        new float[]{0,6/18f,5/18f,4/18f,0,2/18f,1/18f}),

                //PREVENT STEAL LIGHT
                Arguments.of(new MancalaGame(MancalaBoard.PLAYER_A,new int[]{0,1,5,2,11,0,11,0,0,0,2,0,1,0}),
                        new HWrapper(HeuristicFunctions::preventStealLight),
                        new float[]{0,1/3f,2/3f,1/3f,1,0,1}),
                //same as above for the other player
                Arguments.of(new MancalaGame(MancalaBoard.PLAYER_B,new int[]{0,0,0,2,0,1,0,0,1,5,2,11,0,11}),
                        new HWrapper(HeuristicFunctions::preventStealLight),
                        new float[]{0,1/3f,2/3f,1/3f,1,0,1})
        );
    }

    public static class HWrapper implements Heuristic
    {
        Heuristic heuristic;

        private HWrapper(Heuristic h)
        {
            this.heuristic = h;
        }

        @Override
        public void evaluate(MancalaGame game, boolean[] possibleIds, float[] weights)
        {
            heuristic.evaluate(game,possibleIds,weights);
        }
    }
}