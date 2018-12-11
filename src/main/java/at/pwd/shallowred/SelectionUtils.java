package at.pwd.shallowred;

import at.pwd.shallowred.CustomGame.MancalaGame;

import java.util.Random;

public class SelectionUtils
{
    static Random random = new Random(System.nanoTime());

    //TODO may only use light heuristics, since the simulation should be fast
    //TODO switch to endgame database if there are not many stones left
    static int selectMove(MancalaGame game)
    {
        return randomMove(game);
    }

    //TODO switch to endgame database if there are not many stones left
    static int selectExpand(MancalaGame game)
    {
        return randomMove(game);
    }

    static int randomMove(MancalaGame game)
    {
        int selectableMoves = game.getSelectableCount();

        //which one of the selectable Move
        int move = random.nextInt(selectableMoves);

        //get the move-th selectable move
        for(int x = 1;;++x)
        {
            if(game.isSelectable(x))
            {
                if(move==0)
                    return x;
                else
                    --move;
            }
        }
    }
}
