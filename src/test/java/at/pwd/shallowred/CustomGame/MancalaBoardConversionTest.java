package at.pwd.shallowred.CustomGame;

import at.pwd.boardgame.game.mancala.MancalaGame;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

class MancalaBoardConversionTest
{
    private static final int RANDOM_TEST_BOARD_COUNT = 10000;
    private static final int CONVERSION_BOARD_COUNT = 30;
    private static final int STONE_AMOUNT = 72;

    private static final Random r = new Random(100);

    @ParameterizedTest
    @MethodSource("randomBoards")
    void toMancalaGame(MancalaBoard board)
    {
        MancalaGame game = board.toMancalaGame();
        MancalaBoard converted = new MancalaBoard(game);

        Assertions.assertEquals(board,converted);
    }

    @Test
    void generateRandomBoardEverySlotPossible()
    {
        boolean excludeDepots = false;
        //execute two times, once for each boolean value
        do
        {
            boolean result = false;
            for (int i = 0; i < RANDOM_TEST_BOARD_COUNT; ++i)
            {
                MancalaBoard board = MancalaBoard.generateRandomBoard(r, STONE_AMOUNT, excludeDepots);
                result = everySlotFilled(board, excludeDepots);

                //if a board has been generated where every slot has been filled, stop
                if(result)
                    break;
            }

            Assertions.assertTrue(result);

            excludeDepots=!excludeDepots;
        }
        while(excludeDepots);
    }

    @Test
    void generateRandomBoardStoneCount()
    {
        boolean excludeDepots = false;
        //execute two times, once for each boolean value
        do
        {
            for (int i = 0; i < RANDOM_TEST_BOARD_COUNT; ++i)
            {
                MancalaBoard board = MancalaBoard.generateRandomBoard(r, STONE_AMOUNT, excludeDepots);

                //assert sum of stones
                int sum = 0;
                for(int slot = 0;slot<14;++slot)
                    sum+=board.getFields()[slot];
                Assertions.assertEquals(STONE_AMOUNT,sum);

                //assert depot exclusion
                if(excludeDepots)
                {
                    Assertions.assertEquals(0,board.getFields()[MancalaBoard.DEPOT_A]);
                    Assertions.assertEquals(0,board.getFields()[MancalaBoard.DEPOT_B]);
                }

            }

            excludeDepots=!excludeDepots;
        }
        while(excludeDepots);
    }

    boolean everySlotFilled(MancalaBoard board, boolean excludedDepots)
    {
        int[] slots = board.getFields();
        for(int i = 0;i<14;++i)
        {
            if(excludedDepots && (i==MancalaBoard.DEPOT_A || i==MancalaBoard.DEPOT_B))
                continue;

            if(slots[i]==0)
                return false;
        }

        return true;
    }

    private static Stream<Arguments> randomBoards()
    {
        List<Arguments> boards = new ArrayList<>(CONVERSION_BOARD_COUNT);

        int i = 0;
        for(;i<CONVERSION_BOARD_COUNT/2;++i)
            boards.add(Arguments.of(MancalaBoard.generateRandomBoard(r,STONE_AMOUNT,false)));
        for(;i<CONVERSION_BOARD_COUNT;++i)
            boards.add(Arguments.of(MancalaBoard.generateRandomBoard(r,STONE_AMOUNT,true)));

        return boards.stream();
    }
}