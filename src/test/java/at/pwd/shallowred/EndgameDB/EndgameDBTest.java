package at.pwd.shallowred.EndgameDB;

import at.pwd.shallowred.CustomGame.MancalaBoard;
import at.pwd.shallowred.CustomGame.MancalaGame;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

class EndgameDBTest
{

    private static final int RANDOM_TEST_BOARD_COUNT = 10000;
    private static final int[] MINMAX_PLAYER_MULT = new int[]{1,-1};

    private static final Random r = new Random(System.nanoTime());

    @Test
    void loadDBValue()
    {
        List<Float> dbDurations = new ArrayList<>(RANDOM_TEST_BOARD_COUNT);

        EndgameDB.loadDB();

        for(int i = 0;i<RANDOM_TEST_BOARD_COUNT;++i)
        {
            MancalaBoard board = MancalaBoard.generateRandomBoard(r, r.nextInt(EndgameDB.MAX_STONES)+1, true);
            MancalaGame game = new MancalaGame(MancalaBoard.PLAYER_A,board.getFields());
            game.updateWinningState();
            if(game.getWinner()!=MancalaGame.NOBODY)
            {
                --i;
                continue;
            }

            int expected = loadDBValueAndStoreTime(game,dbDurations);

            //play according to db
            while(game.getWinner()==MancalaGame.NOBODY)
            {
                boolean moveFound = false;
                for(int id = 1;id<=6;++id)
                {
                    if(!game.isSelectable(id))
                        continue;

                    MancalaGame child = new MancalaGame().copy(game);
                    child.performTurn(id);
                    if(child.getWinner()==MancalaGame.NOBODY)
                    {
                        int gameDiff = child.getBoard().getFields()[MancalaBoard.DEPOT_A]-child.getBoard().getFields()[MancalaBoard.DEPOT_B];
                        if (loadDBValueAndStoreTime(child,dbDurations)*MINMAX_PLAYER_MULT[child.getCurrentPlayer()]+gameDiff == expected)
                        {
                            game = child;
                            moveFound = true;
                            break;
                        }
                    }
                    else
                    {
                        if(child.getBoard().getFields()[MancalaBoard.DEPOT_A]-child.getBoard().getFields()[MancalaBoard.DEPOT_B]==expected)
                        {
                            game = child;
                            moveFound = true;
                            break;
                        }
                    }


                }

                if(!moveFound)
                {
                    Assertions.fail();
                    return;
                }
            }

            Assertions.assertEquals(expected,game.getBoard().getFields()[MancalaBoard.DEPOT_A]-game.getBoard().getFields()[MancalaBoard.DEPOT_B]);
        }

        //print duration of dbaccess
        System.out.println("DB ACCESS DURATIONS:");
        System.out.println(dbDurations.parallelStream().mapToDouble(v->v).summaryStatistics().toString());
    }

    int loadDBValueAndStoreTime(MancalaGame game, List<Float> durationsInMillis)
    {
        long start = System.nanoTime();
        int value = EndgameDB.loadDBValue(game);
        long duration = System.nanoTime()-start;
        durationsInMillis.add(duration/1000000f);

        return value;

    }

    @ParameterizedTest
    @MethodSource("indexArguments")
    void getIndex(MancalaGame game, long expectedIndexNoOffset)
    {
        int total = game.getBoard().getTotalStones();
        long indexNoOffset = EndgameDB.getIndex(game)-EndgameDB.DB_PART_OFFSETS[total]+EndgameDB.EMPTY_SIDE_OFFSET[total];

        Assertions.assertEquals(expectedIndexNoOffset,indexNoOffset);
    }

    private static Stream<Arguments> indexArguments()
    {
        List<Arguments> args = new ArrayList<Arguments>();
        for(int i = 1;i<14;++i)
        {
            //skip depot
            if(i==MancalaBoard.DEPOT_B)
                continue;
            int expected = i-1-(i>7?1:0);

            int slot = MancalaBoard.getOppositeSlot(i);
            byte[] board = new byte[14];
            board[MancalaBoard.index(MancalaBoard.PLAYER_A,slot)] = 1;
            //set depots to random values, should not have any influence
            board[MancalaBoard.DEPOT_A] = (byte)ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
            board[MancalaBoard.DEPOT_B] = (byte)ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
            args.add(Arguments.of(new MancalaGame(MancalaBoard.PLAYER_A,board),expected));

            //same board mirrored, with other player as current player
            byte[] boardMirrored = new byte[14];
            boardMirrored[MancalaBoard.index(MancalaBoard.PLAYER_B,slot)] = 1;
            //set depots to random values, should not have any influence
            boardMirrored[MancalaBoard.DEPOT_A] = (byte)ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
            boardMirrored[MancalaBoard.DEPOT_B] = (byte)ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
            args.add(Arguments.of(new MancalaGame(MancalaBoard.PLAYER_B,boardMirrored),expected));
        }

        //add hand calculated example
        args.add(Arguments.of(new MancalaGame(MancalaBoard.PLAYER_A,new byte[]{0,2,0,0,0,3,0,0,1,0,0,1,0,0,0}),28229));

        return args.stream();
    }
}