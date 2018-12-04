package at.pwd.shallowred;

import at.pwd.shallowred.CustomGame.MancalaGame;

import java.util.Random;

public class SelectionUtils
{
    static Random random = new Random(System.nanoTime());

    static int selectMove(MancalaGame game)
    {
        return randomMove(game);
    }


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
