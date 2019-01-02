package at.pwd.shallowred.Game;

import at.pwd.shallowred.TestAgents.MancalaAlphaBetaAgent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GameRunner
{
    private static final String CONFIG_PATH = "agentConfigs";
    private static final String CONFIG_NAME = "GameRunnerConfig.json";

    public static void main(String[] args)
    {
        try
        {
            StringBuilder config = new StringBuilder();
            Files.lines(Paths.get(CONFIG_PATH,CONFIG_NAME)).forEach(config::append);
            Path logDir = Files.createTempDirectory("GameRunnerLogs");

            GameUtils.Result result = GameUtils.playAgainst(new ReflectionAgentFactory(MancalaAlphaBetaAgent.class),new ShallowRedFactory(config.toString()),10,5,5,false,logDir);
            //GameUtils.Result result = GameUtils.playAgainst(new ShallowRedFactory(config.toString()),new ReflectionAgentFactory(MancalaMCTSAgent.class),10,5,5,false);
            //GameUtils.Result result = GameUtils.playAgainst(new ReflectionAgentFactory(MancalaAlphaBetaAgent.class),new ShallowRedFactory(config.toString()),1,10,1,false);
            System.out.println(result);

            result = GameUtils.playAgainst(new ShallowRedFactory(config.toString()),new ReflectionAgentFactory(MancalaAlphaBetaAgent.class),10,5,5,false,logDir);
            //result = GameUtils.playAgainst(new ReflectionAgentFactory(MancalaMCTSAgent.class),new ShallowRedFactory(config.toString()),10,5,5,false);
            System.out.println(result);

            System.exit(0);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
            return;
        }
    }
}
