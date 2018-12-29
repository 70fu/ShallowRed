package at.pwd.shallowred.Heuristics;

import at.pwd.shallowred.CustomGame.MancalaBoard;
import at.pwd.shallowred.CustomGame.MancalaGame;

public class HeuristicFunctions
{
    private HeuristicFunctions(){}

    /**
     * 1 if you get an extra turn from it
     * 0 otherwise
     */
    public static void extraTurn(MancalaGame game, boolean[] possibleIds, float[] weights)
    {
        for(int id = 1;id<=6;++id)
        {
            if(!possibleIds[id])
                continue;

            if(game.getStones(id)%13==id)
                weights[id]=1;
            else
                weights[id]=0;
        }
    }

    /**
    Can I steal stones?
    the more the better
    Normalization: divide the number of stones that can be stolen with turn i through the number of stones that can be stolen with all turns together
     */
    public static void stealStones(MancalaGame game, boolean[] possibleIds, float[] weights)
    {
        //calculate number of stones that can be stolen with all stones all together
        int sum = 0;
        for(int id = 1;id<=6;++id)
        {
            if(!possibleIds[id])
                continue;

            //if there are more than 13 stones, then the last stone cannot fall into an empty field, since you make one round
            if(game.getStones(id)>13)
                continue;

            int lastField = game.getLastFieldNoRounds(id);
            //check if the field where the last stone will fall is on current players side and there are no stones in it or if there were exactly 13 stones, thus clearing field with id
            if(lastField<7 && lastField!=0 &&
                    (game.getStones(lastField)==0 || lastField==id))
                sum+=game.getOppositeStones(lastField);
        }

        //check if sum is 0
        if(sum==0)
        {
            for(int id=1;id<=6;++id)
                weights[id]=0;
            return;//we're done here
        }

        //set weights
        for(int id = 1;id<=6;++id)
        {
            if (!possibleIds[id])
                continue;

            //if there are more than 13 stones, then the last stone cannot fall into an empty field, since you make one round
            if (game.getStones(id) > 13)
            {
                weights[id] = 0;
            }
            else
            {
                int lastField = game.getLastFieldNoRounds(id);
                if (lastField < 7 && lastField != 0 &&
                        (game.getStones(lastField) == 0 || lastField == id))
                    weights[id] = (float) game.getOppositeStones(lastField) / (float) sum;
                else
                    weights[id] = 0;
            }
        }
    }

    /**
    A move is good if it moves over the depot (gaining 1 stone)
    Gaining an extra turn is excluded since this is covered by extraTurn()
     1, if a move moves over the depot
     0, otherwise
     */
    public static void moveOverDepot(MancalaGame game, boolean[] possibleIds, float[] weights)
    {
        for (int id = 1; id <= 6; ++id)
        {
            if (!possibleIds[id])
                continue;

            if(game.getStones(id)>id)
                weights[id] = 1;
            else
                weights[id] = 0;
        }
    }

    /**
    Detect patterns, where multiple extra turns in a row can be obtained
     Actually, these patterns are not detected by this heuristic, but to perform such a chain, a player must make the rightmost move, where he/she gets an extra turn
     1, is set for the rightmost possible move where the player gains an extra turn
     0, otherwise
     */
    public static void extraTurnChaining(MancalaGame game, boolean[] possibleIds, float[] weights)
    {
        //iterate from right to left until the rightmost possible move with extra turn is found
        int id = 1;
        for (; id <= 6; ++id)
        {
            if (!possibleIds[id])
                continue;

            if(game.getStones(id)%13==id)//does the player gain an extra turn?
            {
                weights[id] = 1;
                break;
            }
            else
                weights[id] = 0;
        }

        //set rest of the weights to zero
        ++id;
        for (; id <= 6; ++id)
            weights[id] = 0;
    }

    /**
    A Move may be good if the enemy has many stones on the opposite side (opportunity to steal)
     weight[id] = stones on opposite side of id / sum of stones
     */
    public static void stealOpportunity(MancalaGame game, boolean[] possibleIds, float[] weights)
    {
        //calculate sum of stones on opposite side
        int sum = 0;
        for (int id = 1; id <= 6; ++id)
        {
            if (!possibleIds[id])
                continue;

            sum+=game.getOppositeStones(id);
        }

        //check if sum is 0
        if(sum==0)
        {
            for(int id=1;id<=6;++id)
                weights[id]=0;
            return;//we're done here
        }

        //Set weights
        for (int id = 1; id <= 6; ++id)
        {
            if (!possibleIds[id])
                continue;

            weights[id] = (float)game.getOppositeStones(id)/(float) sum;
        }
    }

    /*
    Prevent player from stealing:
    Variations:
        - Fill hole is good
        - Filling hole is only good if there are stones on my side to steal
        - Filling hole is only good if there are stones on my side to steal and the enemy player can move onto the empty field
    Return:
        - 1 if hole can be filled
        - higher value if more stones can be saved (for normalization, see stone stealing)
     */

    /**
     * The more holes a turn may fill on the enemy side, the higher the weight, (ignores holes where there are no stones to steal)
     * weight[id]= holes that are filled / sum of holes + 1/sum of holes if opposite of id is a hole
     */
    public static void preventStealLight(MancalaGame game, boolean[] possibleIds, float[] weights)
    {
        //calculate sum of empty fields on enemy players side
        int sum = 0;
        for (int id = 1; id <= 6; ++id)
        {
            if(game.getOppositeStones(id)==0 && game.getStones(id)>0)
                ++sum;
        }

        //check if sum is 0
        if(sum==0)
        {
            for(int id=1;id<=6;++id)
                weights[id]=0;
            return;//we're done here
        }

        float fraction = 1.0f/sum;
        for (int id = 1; id <= 6; ++id)
        {
            if (!possibleIds[id])
                continue;

            if(game.getStones(id)>11)
                weights[id] = 1;//12 stones guarantee that every field on the enemy side will have a stone in it
            else
            {
                //start with 0
                weights[id] = 0;

                //see how many holes can be filled
                int lastField = game.getLastFieldNoRounds(id);
                if(lastField>id)
                {
                    //if lastField is bigger && on current players side, then all holes have been filled
                    if (lastField < 7)
                    {
                        weights[id] = 1;
                    }
                    else
                    {
                        //see how many holes can be filled
                        for (int enemyId = lastField; enemyId < 14; ++enemyId)
                        {
                            if (game.getStones(enemyId) == 0 && game.getOppositeStones(enemyId)>0)
                                weights[id] += fraction;
                        }

                        //have stones been moved into safety?
                        int oppositeId = MancalaBoard.getOppositeSlot(id);
                        if(oppositeId<lastField && game.getStones(oppositeId)==0)
                            weights[id]+=fraction;
                    }
                }
                else
                {
                    //have stones been moved into safety?
                    int oppositeId = MancalaBoard.getOppositeSlot(id);
                    if(game.getStones(oppositeId)==0)
                        weights[id]+=fraction;
                }
            }
        }
    }

    /*
    Find somehow similar boards in endgame database, by reducing stones at fields where they do not matter (too much)
     */



}
