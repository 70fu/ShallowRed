package at.pwd.shallowred.Game;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GameRunner
{
    private static final String CONFIG_PATH = "out/artifacts/ShallowRedCLI/";
    private static final String CONFIG_NAME = "0975_C050.json";
    private static final String CONFIG_NAME_2 = "0975_C050_DB.json";

    public static void main(String[] args)
    {
        try
        {
            StringBuilder config = new StringBuilder();
            Files.lines(Paths.get(CONFIG_PATH,CONFIG_NAME)).forEach(config::append);

            StringBuilder config2 = new StringBuilder();
            Files.lines(Paths.get(CONFIG_PATH,CONFIG_NAME_2)).forEach(config2::append);
            Path logDir = Files.createTempDirectory("GameRunnerLogs");

            /*EndgameDB.loadDB();

            MancalaGame game = new MancalaGame(MancalaBoard.PLAYER_A,new int[]{0,6,6,6,6,6,6,0,6,6,6,6,6,6});
            while(game.getWinner()==MancalaGame.NOBODY)
            {
                if(EndgameDB.hasValueStored(game))
                {
                    long start = System.nanoTime();
                    int value = EndgameDB.loadDBValue(game);
                    System.out.println(game);
                    System.out.println("Value: "+value);
                    long end = System.nanoTime();
                    System.out.println("Access time: "+(end-start)/1000000f);
                }

                int turn= ThreadLocalRandom.current().nextInt(game.getSelectableCount());
                for(int id = 1;id<=6;id++)
                {
                    if(game.isSelectable(id))
                    {
                        if(turn--==0)
                        {
                            game.performTurn(id);
                            break;
                        }
                    }
                }
            }*/

            GameUtils.Result result = GameUtils.playAgainst(new ShallowRedFactory(config.toString()),new ShallowRedFactory(config2.toString()),1,10,1,false,logDir);
            //GameUtils.Result result = GameUtils.playAgainst(new ShallowRedFactory(config.toString()),new ReflectionAgentFactory(MancalaMCTSAgent.class),10,5,5,false);
            //GameUtils.Result result = GameUtils.playAgainst(new ReflectionAgentFactory(MancalaAlphaBetaAgent.class),new ShallowRedFactory(config.toString()),1,10,1,false);
            System.out.println(result);

            //result = GameUtils.playAgainst(new ShallowRedFactory(config.toString()),new ShallowRedFactory(config2.toString()),25,5,2,false,logDir);
            //result = GameUtils.playAgainst(new ReflectionAgentFactory(MancalaMCTSAgent.class),new ShallowRedFactory(config.toString()),10,5,5,false);
            //System.out.println(result);

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
