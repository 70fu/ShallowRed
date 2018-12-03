package at.pwd.shallowred.CustomGame;

import java.util.ArrayList;
import java.util.List;

public class MancalaBoard {

    private int stonesPerSlot;
    int[] slots;

    public static final int PLAYER_A = 0;
    public static final int PLAYER_B = 1;


    MancalaBoard(){
        stonesPerSlot = 6;

        slots = new int[14];
    }

    int getNumFields(){
        return 14;
    }

    /**
     * returns the depot of the given player.
     * PlayerID = 0 : Simulated AI
     * PlayerID = 1 : Enemy AI
     * @param playerID
     * @return
     */

    int getPlayerDepot(int playerID){
        return slots[6*playerID+1*playerID];
    }


    /**
     * Returns the next slot ID from the given slot "slot". Does not have side effects.
     * @param slot
     * @return
     */
    int next(int slot){
        if(slot < 13){
            return slot+1;
        }else{
            return 0;
        }
    }


    int[] getFields(){
        return slots;
    }

    /**
     * Getter for stones per slot
     * @return Returns the amount of stones per slot at the beginning of the game
     */
    public int getStonesPerSlot() {
        return stonesPerSlot;
    }

    /**
     * Setter for stones per slot. Keep in mind this does not actually alter the slots.
     * @param stonesPerSlot Sets the amount of stones per slot at the beginning of the game.
     */
    public void setStonesPerSlot(int stonesPerSlot) {
        this.stonesPerSlot = stonesPerSlot;
    }

    /**
     * Checks whether the given ID is a valid slot id
     * @param id The questioned slot id
     * @return true if it is a slot, false if it is not a slot (does not exist or is a depot)
     */
    public boolean isSlot(int id) {
        if((id >=0 && id <= 5) || (id >=7 && id <= 12)){
            return true;
        }else{
            return false;
        }
    }

    /**
     * Checks whether the given ID is a valid depot id
     * @param id The questioned slot id
     * @return true if it is a depot, false if it is not a depot (does not exist or is a slot)
     */
    public boolean isDepot(int id) {
        if(id == 6 || id == 13){
            return true;
        }else{
            return false;
        }
    }


}
