package at.pwd.shallowred.Game;

import at.pwd.boardgame.game.mancala.agent.MancalaAgent;

/**
 * Stores the type of an agent, which has a parameterless constructor and needs no initialization.
 * Agents are produced via Reflections
 */
public class ReflectionAgentFactory implements MancalaAgentFactory
{
    private final Class<? extends MancalaAgent> agentType;

    /**
     * Preconditions:
     * @param agentType !=null, has a parameterless constructor
     */
    public ReflectionAgentFactory(Class<? extends MancalaAgent> agentType)
    {
        this.agentType = agentType;
    }

    @Override
    public MancalaAgent produce()
    {
        try
        {
            return agentType.newInstance();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;//this is bad
    }
}
