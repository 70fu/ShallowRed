package at.pwd.shallowred;

import at.pwd.boardgame.game.mancala.MancalaGame;

import java.util.List;

public class SelectionUtils {
    static String selectMove(MancalaGame game, List<String> legalMoves){
        return legalMoves.get(0);
    }


    static String selectExpand(MancalaGame game,List<String> legalMoves){
        return legalMoves.get(0);
    }
}
