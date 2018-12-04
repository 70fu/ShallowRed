package at.pwd.shallowred.CustomGame;

import static at.pwd.shallowred.CustomGame.MancalaBoard.*;

public class MancalaGame {

    private MancalaBoard board;
    private int currentPlayer;
    /**
     * -1, if no winner, otherwise the player id of the winning player
     *  2, for a draw
     */
    private int winner = NOBODY;
    public static final int NOBODY = -1;
    public static final int DRAW = 2;

    public MancalaGame()
    {
        board = new MancalaBoard();
    }

    /**
     * Creates an instance from given game
     * @param game
     */
    public MancalaGame(at.pwd.boardgame.game.mancala.MancalaGame game)
    {
        board = new MancalaBoard(game);

        winner = NOBODY;

        currentPlayer = 0;
    }

    /**
     * Preconditions:
     *      @param other, !=null
     * Postconditions:
     *      Copies given mancala game
     *      @return this
     */
    public MancalaGame copy(MancalaGame other)
    {
        board.copy(other.board);
        currentPlayer = other.currentPlayer;
        winner = other.winner;

        return this;
    }

    /**
     * Preconditions:
     *          @param id The ID of the slot that has been selected, there must be stones in it, 1<=id<=6, getStones(id)>0
     * Postconditions:
     *           Selects the slot with the given ID and calculates the turn.
     *           If this ends the game. The final stones of the enemys player are placed in his/her
     *           depot too.
     *           @return true ... game has ended, false ... game has not been ended
     */
    public boolean performTurn(int id) {
        int slot = board.index(currentPlayer,id);
        int stones = board.clearSlot(slot);
        int[] slots = board.getFields();

        //how many rounds can I make with stones
        int rounds = stones/13;
        //increase stone count in all slots by rounds, except for enemy depot
        for(int x = 0;x<14;++x)
            slots[x]+=rounds;
        int enemyDepot = board.getPlayerDepot(getEnemyPlayer());
        slots[enemyDepot]-=rounds;

        //calculate remaining stones
        stones-=13*rounds;
        //if enemy depot, can be reached with the remaining stones, decrease enemy depot by 1, since it ill be increased with the loop below
        //and grant an extra stone for the loop
        if(stones-id>=7)
        {
            --slots[enemyDepot];
            ++stones;
        }
        //distribute stones until stones run out or slot reaches -1
        for(;--slot>=0 && stones>0;--stones)
        {
            ++slots[slot];
        }
        //wrap around board and distribute remaining stones
        if(stones>0)
        {
            for (slot = 14; stones > 0; --stones)
            {
                --slot;
                ++slots[slot];
            }
        }

        //all stones have been distributed, see if the last stone landed on the depot of the current player
        int playerDepot = board.getPlayerDepot(currentPlayer);

        if(slot!=playerDepot)
        {
            //see if last stone landed on empty slot that belongs to the current player
            if(slots[slot]==1 && board.getOwner(slot)==currentPlayer)
            {
                //add the last stone and stones from the opposite side to current players depot
                int opposite = board.getOppositeSlot(slot);
                slots[playerDepot]+=board.clearSlot(opposite)+board.clearSlot(slot);

                //slots are cleared by above statement
            }

            //switch player
            currentPlayer = getEnemyPlayer();
        }

        return updateWinningState();
    }

    /**
     * Postconditions:
     *      @return id of current player
     */
    public int getCurrentPlayer()
    {
        return currentPlayer;
    }

    /**
     * Postconditions:
     *      @return id of enemy of current player
     */
    public int getEnemyPlayer()
    {
        return 1-currentPlayer;
    }

    /**
     * Preconditions:
     *      @param id, 1<=id<=6
     * Postconditions:
     *      @return true, if current player can perform a turn of given slot
     */
    public boolean isSelectable(int id)
    {
        return getStones(id)>0;
    }

    /**
     * Postconditions:
     *      @return the amount of possible turns for the current player, [0,6]
     */
    public int getSelectableCount()
    {
        int sum = 0;
        for(int x = 1;x<=6;++x)
            if(isSelectable(x))
                ++sum;
        return sum;
    }

    /**
     * Preconditions:
     *      @param id, [0,13]
     * Postconditions:
     *      @return stones of slot with given id
     */
    public int getStones(int id)
    {
        return board.getFields()[board.index(currentPlayer,id)];
    }

    /**
     * Postconditions:
     *      @return -1=nobody yet, 0=Player_A, 1=PLAYER_B, 2=Draw
     */
    public int getWinner()
    {
        return winner;
    }

    /**
     * Postconditions:
     *      if there are no stones on at least one side, all remaining stones are given to the depot of the slot owner
     *      @return true ... game has ended, false ... game has not been ended
     */
    private boolean updateWinningState()
    {
        int[] slots = board.getFields();

        int sumA = 0;
        for(int x = 1;x<7;++x)
            sumA+=slots[x];

        int sumB = 0;
        for(int x = 8;x<14;++x)
            sumB+=slots[x];

        //check if any side has no more stones
        if(sumB==0 || sumA==0)
        {
            //Player A gets remaining stones on his/her side
            for(int x = 1;x<7;++x)
                slots[DEPOT_A]+=board.clearSlot(x);

            //Player B gets remaining stones on his/her side
            for(int x = 8;x<14;++x)
                slots[DEPOT_B]+=board.clearSlot(x);

            //check who is the winner
            if(slots[7]>slots[0])
                winner=PLAYER_B;
            else if(slots[0]>slots[7])
                winner = PLAYER_A;
            else
                winner = DRAW;

            return true;
        }

        return false;
    }

}
