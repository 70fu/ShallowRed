package at.pwd.shallowred.EndgameDB;

import at.pwd.shallowred.CustomGame.MancalaBoard;
import at.pwd.shallowred.CustomGame.MancalaGame;

import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Endgame Database interface for Mancala
 */
public class EndgameDB
{
    /**
     * The DB stores boards until this amount of stones
     */
    public static final int MAX_STONES = 25;
    /**
     * Amount of slot elements used in the permutation representation
     */
    public static final int SLOT_AMOUNT = 12-1;

    /**
     * (n + 5)! / (n! * 5!), where n = amount of stones on board
     * calculated till 42 currently
     */
    public static final long[] EMPTY_SIDE_OFFSET = new long[]{1, 6, 21, 56, 126, 252, 462, 792, 1287, 2002, 3003, 4368, 6188, 8568, 11628, 15504, 20349, 26334, 33649, 42504, 53130, 65780, 80730, 98280, 118755, 142506, 169911, 201376, 237336, 278256, 324632, 376992, 435897, 501942, 575757, 658008, 749398, 850668, 962598, 1086008, 1221759, 1370754, 1533939};

    /**
     * TODO
     * calculated till 42
     */
    public static final long[] DB_PART_OFFSETS = new long[]{0, 0, 6, 63, 371, 1610, 5726, 17640, 48672, 122967, 288925, 638638, 1339702, 2685592, 5173168, 9618940, 17329596, 30347142, 51794988, 86358629, 140943425, 225562610, 354521310, 547977300, 833976780, 1251083925, 1851746715, 2706568956L, 3909689868L, 5585508576L, 7897031760L, 11056169096L, 15336353480L, 21087921932L, 28756759122L, 38906779275L, 52246904463L, 69663288682L, 92257638382L, 121392592048L, 158745244840L, 206370040051L, 266772398137L};

    public static final long[] FACTORIAL_TABLE = new long[]
            {
                    1,//0!
                    1,//1!
                    2,//2!
                    6,//3!
                    24,//4!
                    120,//5!
                    720,//6!
                    5040,//7!
                    40320,//8!
                    362880,//9!
                    3628800//10!
            };

    private static final String PATH_TO_DB = "EndgameDB_ShallowRed.bin";

    private static MappedByteBuffer dbFile;

    private EndgameDB() {}

    /**
     * Client-History-Constraint: needs to be called once before calling loadDBValue
     */
    public static void loadDB()
    {
        RandomAccessFile raf = null;
        try
        {
            raf = new RandomAccessFile(PATH_TO_DB,"r");
            dbFile = raf.getChannel().map(FileChannel.MapMode.READ_ONLY,0,raf.length());
        }
        catch (Exception e)
        {
            e.printStackTrace();//TODO what should we do here, if the endgame database has not been found
        }
    }

    /**
     * Preconditions:
     *      @param game !=null
     * Postconditions:
     *      @return true, if the endgame db has a value stored for given game, false otherwise
     */
    public static boolean hasValueStored(MancalaGame game)
    {
        return game.getBoard().getTotalStones()<=MAX_STONES;
    }

    /**
     * Preconditions:
     *      @param game != null && game.getWinner()==MancalaGame.NOBODY
     * Postconditions:
     *      @return the value of the endgame db for given game in the view of current player
     */
    public static int loadDBValue(MancalaGame game)
    {
        long index = getIndex(game);
        //System.out.println(game);
        int value = dbFile.get((int)index);
        //convert to unsigned
        value = value & 0xFF;
        value -= 128;

        //System.out.println(index);
        //System.out.println(value);
        return value;
    }

    /**
     * This algorithm is based off this stackoverflow answer: https://stackoverflow.com/a/14374455
     * TODO describe adaption
     * Slot is lexicographically less than stone
     * Preconditions:
     *      @param game != null && game.getWinner()==MancalaGame.NOBODY
     * Postconditions:
     *      @return the index of the entry in the endgame db for given game in he view of current player
     */
    public static long getIndex(MancalaGame game)
    {

        int stones = game.getBoard().getTotalStones();
        int slots = SLOT_AMOUNT;
        long index = -EMPTY_SIDE_OFFSET[stones]+DB_PART_OFFSETS[stones];
        for(int id = 1;slots>0;++id)
        {
            //skip enemy depot
            if(id== MancalaBoard.DEPOT_B)
                continue;

            //if there are stones on the field, calculate how many valid terminal nodes are on the left and add to index
            for(int stonesOnField = game.getStones(id);stonesOnField>0;--stonesOnField)
            {
                //calculate (stones+slots-1)! / (stones! * (slots-1)!)

                //calculate (stones+slots-1)!/ max of stones! and (slots-1)!
                //works without overflow with 72 stones on field (= (82!)/(72!))
                int min = Math.min(stones,slots-1);//never higher than SLOT_AMOUNT-1
                int max = Math.max(stones,slots-1);
                long fraction = 1;
                for(int factor = stones+slots-1;factor>max;--factor)
                    fraction*=factor;

                //divide by min of stones! and (slots-1)!
                index+=fraction/FACTORIAL_TABLE[min];

                --stones;
            }

            --slots;
        }

        return index;
    }
}
