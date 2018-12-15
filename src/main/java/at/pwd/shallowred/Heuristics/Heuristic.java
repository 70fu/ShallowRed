package at.pwd.shallowred.Heuristics;

import at.pwd.shallowred.CustomGame.MancalaGame;

//functional heuristic interface
@FunctionalInterface
public interface Heuristic
{
    void evaluate(MancalaGame game, boolean[] possibleIds, float[] weights);
}
