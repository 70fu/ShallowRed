package at.pwd.shallowred;

import at.pwd.boardgame.game.mancala.MancalaGame;

import java.util.ArrayList;

/**
 * All heuristics have the following parameters:
 *      game: the current game
 *      legalMoves: list of legal moves
 *      weights: array of length 6 (because 6 is the maximum of legal moves)
 * All heuristics write values [-1;1] for each legal move and writes it in the array weights at the same index as move in legalMoves
 */
public class HeuristicFunctions
{
    private HeuristicFunctions(){}

    public static void extraTurn(MancalaGame game, ArrayList<String> legalMoves, float weights[])
    {
        String move;
        for(int i = 0;i<legalMoves.size();++i)
        {
            move=legalMoves.get(i);

            //call next stones times
            for(int stones = game.getState().stonesIn(move);stones>0;--stones)
                move = game.getBoard().next(move);

            if(move.equals(game.getBoard().getDepotOfPlayer(game.getState().getCurrentPlayer())))
                weights[i] = 1;
            else
                weights[i] = 0;
        }
    }
}
