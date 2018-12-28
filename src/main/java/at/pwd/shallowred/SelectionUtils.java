package at.pwd.shallowred;

import at.pwd.shallowred.CustomGame.MancalaGame;
import at.pwd.shallowred.Heuristics.Heuristic;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import java.util.Random;

/**
 * Selection algorithm needs to be set, before calling a select method
 */
public class SelectionUtils
{

    public interface SelectionAlgorithm
    {
        /**
         * Since this is the last step of the selection, the select method is ALLOWED TO CHANGE VALUES IN ARRAYS, to prevent copying and such
         * Preconditions:
         *      @param possibleIds != null, length==7, possibleIds[id]==true if id is possible
         *      @param possibleCount amount of elements, that are true in possibleCount, >=1
         *      @param weights !=null, length==7, for each element i: i in [-weightMax,weightMax]
         *      @param weightMax defines maximum and minimum weight value, may be used for normalization of weights
         * Postcondition:
         *      Selects a possible turn id from given parameters
         *      @return id, for which possibleIds[id]==true
         */
        int select(boolean[] possibleIds, int possibleCount, float[] weights, float weightMax);
        JsonObject toJSON();
    }

    public class RouletteWheelSelection implements SelectionAlgorithm
    {
        private RandomSelection randomSelection = new RandomSelection();

        @Override
        public int select(boolean[] possibleIds, int possibleCount, float[] weights, float weightMax)
        {
            //find minimum weight, calculate sum at the same time
            float min = Integer.MAX_VALUE;
            float sum = 0;
            for(int i = 1;i<=6;++i)
            {
                if(possibleIds[i])
                {
                    float weight = weights[i];
                    sum+=weight;
                    if(weight<min)
                        min = weight;
                }
            }

            //add negative minimum weight to all weights to prevent negative weights
            for(int i = 1;i<=6;++i)
                weights[i]-=min;

            //adapt sum to the min correction
            sum+=min*possibleCount;

            //handle special case, where sum==0
            if(sum==0)
                return randomSelection.select(possibleIds,possibleCount,weights,weightMax);

            //select random
            float value = random.nextFloat();
            float cum = 0;
            float iSum = 1.0f/sum;
            for(int i = 1;i<=6;++i)
            {
                if(possibleIds[i])
                {
                    cum+=weights[i]*iSum;
                    if(value<=cum)
                        return i;
                }
            }

            //this should never be reached
            System.err.println("Roulette Wheel Selection failed to select a turn");
            return 0;
        }

        @Override
        public JsonObject toJSON()
        {
            return Json.object().add("type","roulette");
        }
    }

    //deterministic tournament selection
    public class TournamentSelection implements SelectionAlgorithm
    {
        /**
         * Invariant: >=1 && <=6
         */
        private int tournamentSize;

        /**
         * Used for calculation in select, contains the ids chosen for competing
         */
        private int[] tournament;

        public TournamentSelection(int tournamentSize)
        {
            this.tournamentSize = tournamentSize;
            tournament = new int[tournamentSize];
        }

        @Override
        public int select(boolean[] possibleIds, int possibleCount, float[] weights, float weightMax)
        {
            //select tournament
            for(int x = 0;x<tournamentSize;++x)
            {
                int selected = random.nextInt(possibleCount);
                for(int i = 1;;i++)
                {
                    if(possibleIds[i])
                    {
                        if(selected==0)
                        {
                            tournament[x] = i;
                            possibleIds[i] = false;//remove from population
                            --possibleCount;//update possiblecount, since we removed an element from the population
                            break;
                        }
                    }
                    else
                        --selected;
                }
            }

            //return best from tournament
            int best = tournament[0];
            float bestWeight = weights[best];
            for(int x = 1;x<possibleCount && x<tournamentSize;++x)
            {
                int id = tournament[x];
                float weight = weights[id];
                if(weight>=bestWeight)
                {
                    best = id;
                    bestWeight = weight;
                }
            }

            return best;
        }

        @Override
        public JsonObject toJSON()
        {
            return Json.object().add("type","tournament").add("size",tournamentSize);
        }
    }

    public class RandomSelection implements SelectionAlgorithm
    {
        @Override
        public int select(boolean[] possibleIds, int possibleCount, float[] weights, float weightMax)
        {
            //which one of the selectable Move
            int move = random.nextInt(possibleCount);

            //get the move-th selectable move
            for(int x = 1;;++x)
            {
                if(possibleIds[x])
                {
                    if(move==0)
                        return x;
                    else
                        --move;
                }
            }
        }

        @Override
        public JsonObject toJSON()
        {
            return Json.object().add("type","random");
        }
    }

    private Random random = new Random(System.nanoTime());
    private SelectionAlgorithm selectionAlg;
    private boolean[] possibleTurns = new boolean[7];
    private float[] weights = new float[7];
    private float[] weightSum = new float[7];

    /*public SelectionUtils(JsonObject json)
    {
        //TODO
    }*/

    public SelectionUtils()
    {

    }

    public SelectionUtils(SelectionAlgorithm sAlg)
    {
        this.selectionAlg = sAlg;
    }

    public SelectionAlgorithm getSelectionAlg()
    {
        return selectionAlg;
    }

    public void setSelectionAlg(SelectionAlgorithm sAlg)
    {
        this.selectionAlg = sAlg;
    }

    public void setSelectionAlg(JsonObject sAlg)
    {
        String type = sAlg.get("type").asString();
        if(type.equalsIgnoreCase("random"))
            setSelectionAlg(new RandomSelection());
        else if(type.equalsIgnoreCase("roulette"))
            setSelectionAlg(new RouletteWheelSelection());
        else if(type.equalsIgnoreCase("tournament"))
            setSelectionAlg(new TournamentSelection(sAlg.get("size").asInt()));
        else
            throw new IllegalArgumentException("Wrong selector json, must be json object with type: [random, roulette, tournament]");
    }



    //TODO may only use light heuristics in simulation, since the simulation should be fast
    //TODO switch to endgame database if there are not many stones left
    public int select(MancalaGame game, Heuristic[] heuristics, float[] heuristicWeights)
    {
        //reset weights
        for(int x = 1;x<=6;++x)
            weightSum[x] = 0;

        //update possible turns
        for(int id=1;id<=6;++id)
            possibleTurns[id] = game.isSelectable(id);

        //calculate heuristics
        for(int i = 0;i<heuristics.length;++i)
        {
            //calculate heuristic
            heuristics[i].evaluate(game,possibleTurns,weights);

            //scale turn weights
            for(int j = 1;j<=6;++j)
                weights[j]*=heuristicWeights[i];

            //add to weightSum
            for(int j = 1;j<=6;++j)
                weightSum[j]+=weights[j];
        }

        return selectionAlg.select(possibleTurns,game.getSelectableCount(),weightSum,heuristics.length);
    }
}
