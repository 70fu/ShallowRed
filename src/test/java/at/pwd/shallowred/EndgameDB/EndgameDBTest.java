package at.pwd.shallowred.EndgameDB;

import at.pwd.shallowred.CustomGame.MancalaBoard;
import at.pwd.shallowred.CustomGame.MancalaGame;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

class EndgameDBTest
{

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
            int[] board = new int[14];
            board[MancalaBoard.index(MancalaBoard.PLAYER_A,slot)] = 1;
            //set depots to random values, should not have any influence
            board[MancalaBoard.DEPOT_A] = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
            board[MancalaBoard.DEPOT_B] = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
            args.add(Arguments.of(new MancalaGame(MancalaBoard.PLAYER_A,board),expected));

            //same board mirrored, with other player as current player
            int[] boardMirrored = new int[14];
            boardMirrored[MancalaBoard.index(MancalaBoard.PLAYER_B,slot)] = 1;
            //set depots to random values, should not have any influence
            boardMirrored[MancalaBoard.DEPOT_A] = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
            boardMirrored[MancalaBoard.DEPOT_B] = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
            args.add(Arguments.of(new MancalaGame(MancalaBoard.PLAYER_B,boardMirrored),expected));
        }

        //add hand calculated example
        args.add(Arguments.of(new MancalaGame(MancalaBoard.PLAYER_A,new int[]{0,2,0,0,0,3,0,0,1,0,0,1,0,0,0}),28229));

        return args.stream();
    }
}