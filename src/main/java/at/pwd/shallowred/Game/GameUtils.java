package at.pwd.shallowred.Game;

import at.pwd.shallowred.CustomGame.MancalaBoard;
import at.pwd.shallowred.CustomGame.MancalaGame;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

//Utility class to play games between agents, can also play games in parallel
public class GameUtils
{
    public static class Result
    {
        public int timesWonA;
        public int timesWonB;
        public int timesDraw;
        public int timesError;

        public int getValidGames()
        {
            return timesWonA+timesDraw+timesWonB;
        }

        @Override
        public String toString()
        {
            JsonObject root = Json.object();
            root.add("timesWonA",timesWonA);
            root.add("timesWonB",timesWonB);
            root.add("timesDraw",timesDraw);
            root.add("timesError",timesError);

            return root.toString(WriterConfig.PRETTY_PRINT);
        }
    }

    private static class RemainingPlays
    {
        private MancalaAgentFactory agentA;
        private MancalaAgentFactory agentB;
        private int remaining;
        private int computingTime;
        private Path logDir;
        private MancalaBoard board;

        private final String dateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);


        public RemainingPlays(MancalaAgentFactory agentA, MancalaAgentFactory agentB, int computingTime, int games, Path logDir, MancalaBoard board)
        {
            this.agentA = agentA;
            this.agentB = agentB;
            this.remaining = games;
            this.computingTime = computingTime;
            this.logDir = logDir;
            this.board = board;
        }

        /**
         * Postconditions:
         *      @return A configured Game thread if another game should be played, null if no games should be played anymore
         */
        public synchronized GameThread generateGameThread()
        {
            if(remaining==0)
                return null;

            //instantiate and configure game thread
            GameThread gameThread = new GameThread(agentA.produce(),agentB.produce(),computingTime,board.toMancalaGame());

            //if log directory is given, create log file and create BufferedWriter
            if(logDir!=null)
            {
                Path logFile = Paths.get(logDir.toString(),remaining+"_"+dateTime+".log");
                try
                {
                    logFile = Files.createFile(logFile);
                    gameThread.setLogWriter(Files.newBufferedWriter(logFile));
                }
                catch (IOException e)
                {
                    System.err.println("Log file "+logFile+" could not be created. Continuing without logging");
                }
            }

            //decrease remaining
            --remaining;
            return gameThread;
        }

        /**
         * Postconditions:
         *      @return A configured Game thread, does not decrease the remaining play count
         */
        public synchronized GameThread regenerateGameThread()
        {
            ++remaining;
            return generateGameThread();
        }
    }

    /**
     * Calls playAgainst with default board (6 stones per slot)
     * @param agentA !=null
     * @param agentB !=null
     * @param games >=0
     * @param threads >=1
     * @param repeatOnError
     * @param logDir must be a directory or null
     * Postconditions:
     *      games (<-parameter) games are played between given mancala agents, may be played in parallel
     *      if repeatOnError is true, then the game is repeated if it ended due to an error
     *      @return result of the games
     */
    public static Result playAgainst(MancalaAgentFactory agentA, MancalaAgentFactory agentB,int games, int computingTime, int threads, boolean repeatOnError, Path logDir) throws InterruptedException
    {
        return playAgainst(agentA, agentB, games, computingTime, threads, repeatOnError, logDir,new MancalaBoard(6));
    }

    /**
     *
     * @param agentA !=null
     * @param agentB !=null
     * @param games >=0
     * @param threads >=1
     * @param repeatOnError
     * @param logDir must be a directory or null
     * @param board !=null
     * Postconditions:
     *      games (<-parameter) games are played between given mancala agents, on given board, may be played in parallel
     *      if repeatOnError is true, then the game is repeated if it ended due to an error
     *      @return result of the games
     */
    public static Result playAgainst(MancalaAgentFactory agentA, MancalaAgentFactory agentB,int games, int computingTime, int threads, boolean repeatOnError, Path logDir, MancalaBoard board) throws InterruptedException
    {
        Result result = new Result();
        RemainingPlays rp = new RemainingPlays(agentA,agentB,computingTime,games, logDir, board);
        GameThreadWatcher[] watcherThreads = new GameThreadWatcher[threads];
        for(int x = 0;x<threads;++x)
        {
            GameThreadWatcher watcher = new GameThreadWatcher(result, rp, repeatOnError);
            watcherThreads[x] = watcher;
            watcher.start();

        }

        //wait for watcher threads
        for(int x = 0;x<threads;++x)
        {
            try
            {
                watcherThreads[x].join();
            }
            catch (InterruptedException e)
            {
                //interrupt all watchers
                for(GameThreadWatcher w : watcherThreads)
                    w.interrupt();

                throw new InterruptedException();
            }
        }

        return result;
    }

    private static class GameThreadWatcher extends Thread
    {
        private final Result result;
        private final RemainingPlays remaining;
        private final boolean repeatOnError;

        private GameThread gameThread;

        public GameThreadWatcher(Result result, RemainingPlays remaining, boolean repeatOnError)
        {
            this.result = result;
            this.remaining = remaining;
            this.repeatOnError = repeatOnError;
        }

        @Override
        public void run()
        {
            //are there still games to play?
            while(!isInterrupted() && (gameThread = remaining.generateGameThread())!=null)
            {
                while(true)
                {
                    //play game
                    gameThread.start();
                    try
                    {
                        gameThread.join();
                    }
                    catch (InterruptedException e)
                    {
                        //interrupt game thread
                        gameThread.interrupt();
                        return;
                    }

                    //check result
                    switch(gameThread.getResult())
                    {
                        case MancalaGame.DRAW:
                            synchronized(result){++result.timesDraw;}
                            break;
                        case MancalaBoard.PLAYER_A:
                            synchronized(result){++result.timesWonA;}
                            break;
                        case MancalaBoard.PLAYER_B:
                            synchronized(result){++result.timesWonB;}
                            break;
                        case MancalaGame.NOBODY:
                            synchronized(result){++result.timesError;}
                            break;

                    }

                    //if an error occured and error games should be replayed -> play again
                    if(gameThread.getResult()==MancalaGame.NOBODY && repeatOnError)
                        gameThread = remaining.regenerateGameThread();
                    else
                        break;
                }

                //close logWriter
                if(gameThread.getLogWriter()!=null)
                {
                    try
                    {
                        gameThread.getLogWriter().close();
                    }
                    catch(IOException e)
                    {
                        System.err.println("Failed to close log file");
                    }
                }
            }
        }
    }
}
