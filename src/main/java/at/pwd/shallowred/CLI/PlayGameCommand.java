package at.pwd.shallowred.CLI;

import at.pwd.shallowred.CustomGame.MancalaBoard;
import at.pwd.shallowred.Game.GameUtils;
import at.pwd.shallowred.Game.MancalaAgentFactory;
import at.pwd.shallowred.Game.ReflectionAgentFactory;
import at.pwd.shallowred.Game.ShallowRedFactory;
import at.pwd.shallowred.TestAgents.MancalaAlphaBetaAgent;
import at.pwd.shallowred.TestAgents.MancalaMCTSAgent;
import picocli.CommandLine.*;
import picocli.CommandLine.Model.CommandSpec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

@Command(name="play",description="Plays a game between two AIs. Outputs (per default) W if player wins against enemy, L on loss and D on draw. If ShallowRed is specified as player then a config must be specified either by -p and passing the json as string or by --playerConfigFile and passing a path to json file (-e and --enemyConfigFile for enemy)")
public class PlayGameCommand implements Callable<Void>
{
    private static final int MAX_REPEATS = 255;
    private static final int STONES_PER_SLOT = 6;
    private static int NUM_STONES = STONES_PER_SLOT*12;

    public enum Agent
    {
        MCTS,
        ALPHABETA,
        SHALLOWRED
    }

    @Option(names={"-t","--computingTime"}, description="The number of seconds each agent is allowed to think (default: ${DEFAULT-VALUE})")
    private int computingTime = 10;

    @Option(names={"--enemyStarts"}, description = "Whether or not the enemy starts. Ignored if --switchSides is given.\n (default: ${DEFAULT-VALUE})")
    private boolean enemyStarts = false;

    @Option(names={"-r","--repeatOnError"}, description = "Repeats game if an error occured (default: ${DEFAULT-VALUE})")
    private boolean repeatOnError = false;
    @Option(names={"--repeatOnDraw"}, description = "Repeats game, if an error occured. (default: ${DEFAULT-VALUE})")
    private boolean repeatOnDraw = false;

    @Option(names="--winChar",description="Character printed to stdout when player wins (default: ${DEFAULT-VALUE})")
    private char winChar = 'W';
    @Option(names="--lossChar",description="Character printed to stdout when player loses (default: ${DEFAULT-VALUE})")
    private char lossChar = 'L';
    @Option(names="--drawChar",description="Character printed to stdout when a draw occurs (default: ${DEFAULT-VALUE})")
    private char drawChar = 'D';

    //random board parameters
    @Option(names="--randomBoard", description="Takes a board with random stone distribution for play.\n Should probably be combined with --switchSides/-s option.\n (default: ${DEFAULT-VALUE})")
    private boolean randomBoard = false;

    @Option(names={"-s","--switchSides"}, description = "Plays two games, with the same board, but the ais switch places on the second game.\n In order for an ai to win, it needs to win on both sides, only 1 win counts as a draw\n (default: ${DEFAULT-VALUE})")
    private boolean switchSides = false;
    @Option(names={"--repeatOnSameSideWin"}, description = "Repeats game, if the same side wins twice. Only considered if side switching is active (-s or --switchSides)\n (default: ${DEFAULT-VALUE})")
    private boolean repeatOnSameSideWin = false;

    @Parameters(index="0", description="Agent that competes against enemy. Valid values: ${COMPLETION-CANDIDATES}")
    private Agent player;
    @Parameters(index="1", description="Enemy agent, valid values: ${COMPLETION-CANDIDATES}")
    private Agent enemy;

    //configs for shallowred agents
    @Option(names={"-p","--playerConfig"},description="Used when player agent is ShallowRed. Must be a quoted json string (quotes in json string must have \\). If not specified, config is tried to load using the --playerConfigFile argument.")
    private String playerConfig;
    @Option(names={"-e","--enemyConfig"},description="Used when enemy agent is ShallowRed. Must be a quoted json string (quotes in json string must have \\). If not specified, config is tried to load using the --enemyConfigFile argument.")
    private String enemyConfig;
    @Option(names="--playerConfigFile",description="Path to json config for player agent. Used when player agent is ShallowRed and no config is set via -p or --playerConfig")
    private File playerConfigFile;
    @Option(names="--enemyConfigFile",description="Path to json config for enemy agent. Used when enemy agent is ShallowRed and no config is set via -e or --enemyConfig")
    private File enemyConfigFile;

    @Option(names={"-l","--logDirectory"},description="Logs all moves taken by the agents into files created in given directory.")
    private Path logDirPath;

    @Spec
    private CommandSpec commandSpec;


    @Override
    public Void call() throws Exception
    {
        MancalaAgentFactory playerAgent = constructAgent(player,playerConfig,playerConfigFile);
        MancalaAgentFactory enemyAgent = constructAgent(enemy,enemyConfig,enemyConfigFile);

        //check log directory, create log directory if it does not exist
        if(logDirPath!=null)
        {
            if(!Files.exists(logDirPath))
                Files.createDirectories(logDirPath);

            if(!Files.isDirectory(logDirPath))
                throw new ParameterException(commandSpec.commandLine(), "Given logDirectory is not a directory: " + logDirPath.toString());
        }

        //construct Board
        MancalaBoard board;
        if(randomBoard)
            board = MancalaBoard.generateRandomBoard(ThreadLocalRandom.current(),NUM_STONES,true);
        else//Load normal starting board with 6 stones per field
            board = new MancalaBoard(STONES_PER_SLOT);

        char resultChar = drawChar;
        //used for repeatOnSameSideWin
        boolean repeat;
        int repeatCount = 0;
        do
        {
            repeat = false;

            if (!switchSides)
            {
                GameUtils.Result result;
                if (enemyStarts)
                    result = GameUtils.playAgainst(enemyAgent, playerAgent, 1, computingTime, 1, repeatOnError, logDirPath, board);
                else
                    result = GameUtils.playAgainst(playerAgent, enemyAgent, 1, computingTime, 1, repeatOnError, logDirPath, board);

                //analyze result and print character
                if (result.timesDraw == 1)
                    resultChar = drawChar;
                else if (result.timesError == 1)
                {
                    System.err.println("An agent error occured during the game");
                    break;
                }
                else if (result.timesWonA == 1)
                    resultChar = enemyStarts ? lossChar : winChar;
                else if (result.timesWonB == 1)
                    resultChar = enemyStarts ? winChar : lossChar;
            }
            else
            {
                GameUtils.Result result = GameUtils.playAgainst(playerAgent,enemyAgent,1,computingTime,1,repeatOnError,logDirPath, board);
                GameUtils.Result result2 = GameUtils.playAgainst(enemyAgent,playerAgent,1,computingTime,1,repeatOnError,logDirPath, board);

                //add result2 to result (A & B are switched)
                result.timesWonA += result2.timesWonB;
                result.timesWonB += result2.timesWonA;
                result.timesDraw += result2.timesDraw;
                result.timesError += result2.timesError;

                //analyze result and print character
                if(result.timesError>0)
                {
                    System.err.println("An agent error occured during the game");
                    break;
                }
                else if(result.timesDraw==2)
                    resultChar = drawChar;
                else if(result.timesWonA==result.timesWonB)
                {
                    if(repeatOnSameSideWin)
                        repeat = true;
                    else
                        resultChar=drawChar;
                }
                else if(result.timesWonA>result.timesWonB)
                    resultChar = winChar;
                else //if(result.timesWonA<result.timesWonB
                    resultChar = lossChar;
            }
        }
        while(((resultChar==drawChar && repeatOnDraw) || repeat) && repeatCount++<MAX_REPEATS);

        //print result
        if(repeatCount<MAX_REPEATS)
            System.out.println(resultChar);

        return null;
    }

    private MancalaAgentFactory constructAgent(Agent type, String config, File configFile) throws Exception
    {
        if(type==Agent.MCTS)
            return new ReflectionAgentFactory(MancalaMCTSAgent.class);
        else if(type == Agent.ALPHABETA)
            return new ReflectionAgentFactory(MancalaAlphaBetaAgent.class);
        else /*if(type==Agent.SHALLOWRED)*/
        {
            //try to load config from file if no config is given
            if(config==null)
            {
                if(configFile==null || !configFile.exists())
                    throw new ParameterException(commandSpec.commandLine(),"Config file "+configFile.getAbsolutePath()+" for ShallowRed agent not found.");

                StringBuilder configBuilder = new StringBuilder();
                try(BufferedReader br = new BufferedReader(new FileReader(configFile)))
                {
                    br.lines().forEach(configBuilder::append);
                }
                config = configBuilder.toString();
            }

            return new ShallowRedFactory(config);
        }
    }
}
