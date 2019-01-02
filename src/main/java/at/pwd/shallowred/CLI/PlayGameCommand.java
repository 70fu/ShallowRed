package at.pwd.shallowred.CLI;

import at.pwd.shallowred.Game.GameUtils;
import at.pwd.shallowred.Game.MancalaAgentFactory;
import at.pwd.shallowred.Game.ReflectionAgentFactory;
import at.pwd.shallowred.Game.ShallowRedFactory;
import at.pwd.shallowred.TestAgents.MancalaAlphaBetaAgent;
import at.pwd.shallowred.TestAgents.MancalaMCTSAgent;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Model.CommandSpec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name="play",description="Plays a game between two AIs. Outputs (per default) W if player wins against enemy, L on loss and D on draw. If ShallowRed is specified as player then a config must be specified either by -p and passing the json as string or by --playerConfigFile and passing a path to json file (-e and --enemyConfigFile for enemy)")
public class PlayGameCommand implements Callable<Void>
{
    public enum Agent
    {
        MCTS,
        ALPHABETA,
        SHALLOWRED
    }

    @Option(names={"-t","--computingTime"}, description="The number of seconds each agent is allowed to think (default: ${DEFAULT-VALUE})")
    private int computingTime = 10;

    @Option(names={"--enemyStarts"}, description = "Whether or not the enemy starts (default: ${DEFAULT-VALUE})")
    private boolean enemyStarts = false;

    @Option(names={"-r","--repeatOnError"}, description = "Repeats game if an error occured (default: ${DEFAULT-VALUE})")
    private boolean repeatOnError = false;

    @Option(names="--winChar",description="Character printed to stdout when player wins (default: ${DEFAULT-VALUE})")
    private char winChar = 'W';
    @Option(names="--lossChar",description="Character printed to stdout when player loses (default: ${DEFAULT-VALUE})")
    private char lossChar = 'L';
    @Option(names="--drawChar",description="Character printed to stdout when a draw occurs (default: ${DEFAULT-VALUE})")
    private char drawChar = 'D';

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
            if(!Files.isDirectory(logDirPath))
                throw new ParameterException(commandSpec.commandLine(), "Given logDirectory is not a directory: " + logDirPath.toString());

            if(!Files.exists(logDirPath))
                Files.createDirectories(logDirPath);
        }

        GameUtils.Result result;
        if(enemyStarts)
            result = GameUtils.playAgainst(enemyAgent,playerAgent,1,computingTime,1,repeatOnError, logDirPath);
        else
            result = GameUtils.playAgainst(playerAgent,enemyAgent,1,computingTime,1,repeatOnError, logDirPath);

        //analyze result and print character
        if(result.timesDraw==1)
            System.out.println(drawChar);
        else if(result.timesError==1)
            System.err.println("An agent error occured during the game");
        else if(result.timesWonA==1)
            System.out.println(enemyStarts?lossChar:winChar);
        else if(result.timesWonB==1)
            System.out.println(enemyStarts?winChar:lossChar);

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
                    throw new IllegalArgumentException("Missing config for ShallowRed agent");

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
