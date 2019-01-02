package at.pwd.shallowred.tests;

import at.pwd.shallowred.CustomGame.MancalaBoard;
import at.pwd.shallowred.CustomGame.MancalaGame;
import at.pwd.shallowred.Heuristics.Heuristic;
import at.pwd.shallowred.SelectionUtils;
import com.eclipsesource.json.JsonObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;


class SelectionUtilsTest
{
    private static final Heuristic[] HEURISTICS = new Heuristic[]{
            new StaticHeuristic(new float[] {0,0.4f,0.2f,0.8f,0.1f,0.16f,0}),
            new StaticHeuristic(new float[]{0,1,-1,0.8f,-0.8f,0.6f,-0.6f})
    };
    private static final float[] HEURISTIC_WEIGHTS = new float[]{0.25f,0.5f};
    private static final float IMPOSSIBLE_WEIGHT = Float.MIN_VALUE;
    private static final float[] EXPECTED = new float[]{
            0,
            HEURISTIC_WEIGHTS[0]*0.4f + HEURISTIC_WEIGHTS[1]*1,
            HEURISTIC_WEIGHTS[0]*0.2f + HEURISTIC_WEIGHTS[1]*-1,
            HEURISTIC_WEIGHTS[0]*0.8f + HEURISTIC_WEIGHTS[1]*0.8f,
            IMPOSSIBLE_WEIGHT,//HEURISTIC_WEIGHTS[0]*0*0.1f + HEURISTIC_WEIGHTS[1]*0*-0.8f,
            HEURISTIC_WEIGHTS[0]*0.16f + HEURISTIC_WEIGHTS[1]*0.6f,
            IMPOSSIBLE_WEIGHT//HEURISTIC_WEIGHTS[0]*0*0 + HEURISTIC_WEIGHTS[1]*0*-0.6f,
    };

    private static SelectionUtils selector;
    private static MancalaGame game;
    private static MancalaGame mirrored;
    private static final boolean[] SELECTION_FILTER = new boolean[]{true,true,true,true,true,true,true};

    @BeforeAll
    static void setup()
    {
        game = new MancalaGame(MancalaBoard.PLAYER_A,new int[]{0,1,2,3,0,5,0,7,8,9,10,11,12,13});
        mirrored = new MancalaGame(MancalaBoard.PLAYER_B,new int[]{7,8,9,10,11,12,13,0,1,2,3,0,5,0});
        selector = new SelectionUtils();
        //set selection algorithm that takes the move with maximum weight
        selector.setSelectionAlg(new SelectionUtils.SelectionAlgorithm()
        {
            @Override
            public int select(boolean[] possibleIds, int possibleCount, float[] weights, float weightMax)
            {
                int max = 0;
                float maxWeight = Float.MIN_VALUE;
                for(int id = 1;id<=6;++id)
                {
                    if(!possibleIds[id])
                    {
                        weights[id]=IMPOSSIBLE_WEIGHT;//set weights of impossible turns to a specific value
                        continue;
                    }

                    if(weights[id]>maxWeight)
                    {
                        max = id;
                        maxWeight = weights[id];
                    }
                }

                //assert weights
                assertArrayEquals(EXPECTED,weights);

                return max;
            }

            @Override
            public JsonObject toJSON()
            {
                throw new NotImplementedException();
            }
        });
    }

    @Test
    void select()
    {
        int gameChoice = selector.select(game,HEURISTICS,HEURISTIC_WEIGHTS,SELECTION_FILTER);
        assertEquals(1,gameChoice);

        int mirroredChoice = selector.select(mirrored,HEURISTICS,HEURISTIC_WEIGHTS,SELECTION_FILTER);
        assertEquals(1,mirroredChoice);
    }

    private static class StaticHeuristic implements Heuristic
    {
        private final float[] weightValues;
        public StaticHeuristic(float[] weightValues)
        {
            this.weightValues = Arrays.copyOf(weightValues,7);
        }

        @Override
        public void evaluate(MancalaGame game, boolean[] possibleIds, float[] weights)
        {
            for(int id = 0;id<weightValues.length;++id)
                weights[id] = weightValues[id];
        }
    }
}