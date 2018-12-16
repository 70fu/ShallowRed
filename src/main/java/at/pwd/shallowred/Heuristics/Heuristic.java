package at.pwd.shallowred.Heuristics;

import at.pwd.shallowred.CustomGame.MancalaGame;

//functional heuristic interface
@FunctionalInterface
public interface Heuristic
{
    /**
     * Preconditions:
     *      @param game !=null
     *      @param possibleIds !=null && length==7 (index 0 is unused), possibleIds[id]==true, if current player can make a turn with given id
     *      @param weights !=null && length==7 (index 0 is unused)
     * Postcondition:
     *      If turn id is possible, the evaluate function MUST write a value in weights[id] between [-1;1], meaning 1=very good turn, -1=very bad turn
     */
    void evaluate(MancalaGame game, boolean[] possibleIds, float[] weights);
}
