package at.pwd.shallowred.CustomGame;

import java.util.Arrays;

/**
 * Mancalaboard custom implementation.
 *
 * 
 *  Mancalaboard ID distribution;
 *
 *       (  8 )|(  9 )|( 10 )|( 11 )|( 12 )|( 13 )
 *  ( 7 )                                         ( 0 )
 *       (  6 )|(  5 )|(  4 )|(  3 )|(  2 )|(  1 )
 */


public class MancalaBoard {

    private int[] slots;

    public static final int PLAYER_A = 0;
    public static final int PLAYER_B = 1;
    public static final int DEPOT_A = 0;
    public static final int DEPOT_B = 7;


    /**
     * Creates an empty MancalaBoard
     */
    public MancalaBoard(){
        slots = new int[14];
    }

    /**
     * used for testing
     * Preconditions:
     *      @param slots, must have length 14
     */
    public MancalaBoard(int[] slots)
    {
        this.slots = slots;
    }

    /**
     * Creates a mancalaboard from given game
     *
     * Because the API is HORRIBLE!
     */
    public MancalaBoard(at.pwd.boardgame.game.mancala.MancalaGame game)
    {
        this();
        
        //TODO: TEST THIS!

        String slotID = game.getBoard().getDepotOfPlayer(0);
        slots[0] = game.getState().stonesIn(slotID);

        for(int i = 13; i >= 1;i--){
            slotID = game.getBoard().next(slotID);
            slots[i] = game.getState().stonesIn(slotID);
        }


    }

    /**
     * Postconditions:
     *      copies the slot values of given board
     *      @return this
     */
    public MancalaBoard copy(MancalaBoard other)
    {
        for(int x = 0;x<14;++x)
            slots[x] = other.slots[x];
        return this;
    }



    /**
     * Returns the next slot ID from the given slot "slot". Does not have side effects.
     * @param slot
     * @return
     */
    int next(int slot){
        --slot;
        if(slot==-1)
            return 13;
        return slot;
    }

    /**
     * Preconditions:
     *      @param playerId, 0 or 1
     * Postconditions:
     *      @return the slot index of the given player
     */
    public int getPlayerDepot(int playerId)
    {
        return playerId*7;
    }


    public int[] getFields(){
        return slots;
    }

    /**
     * Preconditions:
     *      @param slot, [0,13]
     * Postconditions:
     *      @return true if given slot is a depot, false otherwise
     */
    public boolean isDepot(int slot) {
        return slot==DEPOT_A || slot==DEPOT_B;
    }

    /**
     * Preconditions:
     *      @param slot, [0,13]
     * Postconditions:
     *      @return owner of given slot
     */
    public int getOwner(int slot)
    {
        return (slot>6)?PLAYER_B:PLAYER_A;
    }

    /**
     * Preconditions:
     *      @param slot, [0,13], also works for ids (player independent)
     * Postconditions:
     *      @return slot index of the opposite slot
     */
    public static int getOppositeSlot(int slot)
    {
        return 14-slot;
    }

    /**
     * Preconditions:
     *      @param slot, [0,13]
     * Postconditions:
     *      sets number of stones of given slot to 0
     *      @return the number of stones that were in given slot before
     */
    public int clearSlot(int slot)
    {
        int tmp = slots[slot];
        slots[slot] = 0;
        return tmp;
    }

    /**
     * Preconditions:
     *      @param playerId, 0 or 1
     *      @param id, must be a valid id
     * Postconditions:
     *      @return the slot index for given id for given player
     */
    public static int index(int playerId, int id)
    {
        int i = id+playerId*7;
        if(i>13)
            return i-14;
        else
            return i;
    }

    @Override
    public boolean equals(Object obj)
    {
        if(obj==null || obj.getClass()!=getClass())
            return false;

        MancalaBoard other = (MancalaBoard) obj;
        return Arrays.equals(slots,other.slots);
    }

    @Override
    public int hashCode()
    {
        return Arrays.hashCode(slots);
    }

    @Override
    public String toString()
    {
        return toString('|');
    }

    public String toString(char edgeCharacter)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%c      (%3d ) (%3d ) (%3d ) (%3d ) (%3d ) (%3d )      %c",edgeCharacter,slots[8],slots[9],slots[10],slots[11],slots[12],slots[13],edgeCharacter));
        builder.append(System.lineSeparator());
        builder.append(String.format("%c(%3d )                                         (%3d )%c",edgeCharacter,slots[7],slots[0],edgeCharacter));
        builder.append(System.lineSeparator());
        builder.append(String.format("%c      (%3d ) (%3d ) (%3d ) (%3d ) (%3d ) (%3d )      %c",edgeCharacter,slots[6],slots[5],slots[4],slots[3],slots[2],slots[1],edgeCharacter));
        return builder.toString();
    }
}
