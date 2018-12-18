package at.pwd.shallowred.Game;

import at.pwd.shallowred.TestAgents.MancalaMCTSAgent;

public class GameRunner
{
    public static void main(String[] args)
    {
        try
        {
            GameUtils.Result result = GameUtils.playAgainst(new ShallowRedFactory("{\"selector\":{\"type\":\"random\"},\"expand\":[],\"simulation\":[]}"),new ReflectionAgentFactory(MancalaMCTSAgent.class),1,10,1,false);
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
