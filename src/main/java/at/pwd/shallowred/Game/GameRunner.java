package at.pwd.shallowred.Game;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GameRunner
{
    private static final String CONFIG_PATH = "agentConfigs";
    private static final String CONFIG_NAME = "RandomConfig.json";
    private static final String CONFIG_NAME_2 = "RandomConfig2.json";

    public static void main(String[] args)
    {
        try
        {
            StringBuilder config = new StringBuilder();
            Files.lines(Paths.get(CONFIG_PATH,CONFIG_NAME)).forEach(config::append);

            StringBuilder config2 = new StringBuilder();
            Files.lines(Paths.get(CONFIG_PATH,CONFIG_NAME_2)).forEach(config2::append);
            Path logDir = Files.createTempDirectory("GameRunnerLogs");

            GameUtils.Result result = GameUtils.playAgainst(new ShallowRedFactory(config2.toString()),new ShallowRedFactory(config.toString()),25,5,2,false,logDir);
            //GameUtils.Result result = GameUtils.playAgainst(new ShallowRedFactory(config.toString()),new ReflectionAgentFactory(MancalaMCTSAgent.class),10,5,5,false);
            //GameUtils.Result result = GameUtils.playAgainst(new ReflectionAgentFactory(MancalaAlphaBetaAgent.class),new ShallowRedFactory(config.toString()),1,10,1,false);
            System.out.println(result);

            result = GameUtils.playAgainst(new ShallowRedFactory(config.toString()),new ShallowRedFactory(config2.toString()),25,5,2,false,logDir);
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
