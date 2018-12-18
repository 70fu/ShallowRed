package at.pwd.shallowred.Game;

import at.pwd.boardgame.game.mancala.agent.MancalaAgent;
import at.pwd.shallowred.ShallowRed;

/**
 * Produces ShallowRed agents using given configuration in json format
 */
public class ShallowRedFactory implements MancalaAgentFactory
{
    private final String jsonConfig;

    /**
     * Preconditions:
     *      @param jsonConfig !=null
     */
    public ShallowRedFactory(String jsonConfig)
    {
        this.jsonConfig = jsonConfig;
    }

    @Override
    public MancalaAgent produce()
    {
        return new ShallowRed(jsonConfig);
    }
}
