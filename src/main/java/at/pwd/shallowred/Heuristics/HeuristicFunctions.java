package at.pwd.shallowred.Heuristics;

import at.pwd.shallowred.CustomGame.MancalaGame;

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

    /**
     * 1 if you get an extra turn from it
     * 0 otherwise
     */
    public static void extraTurn(MancalaGame game, boolean[] possibleIds, float[] weights)
    {
        //TODO
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

    /*
    Can I steal stones?
    the more the better
    Normalization: divide the number of stones that can be stolen with turn i through the number of stones that can be stolen with all turns together
     */

    /*
    Detect patterns, where multiple extra turns in a row can be obtained
     */

    /*
    Find somehow similar boards in endgame database, by reducing stones at fields where they do not matter (too much)
     */

    /*
    A move is good if it moves over the depot (gaining 1 stone)
    Gaining an extra turn should maybe be excluded since this is covered by another heuristic
     */

    /*
    A Move may be good if the enemy has many stones on the opposite side (opportunity to steal)
     */
}
