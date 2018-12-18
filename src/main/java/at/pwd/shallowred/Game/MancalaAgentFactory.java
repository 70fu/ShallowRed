package at.pwd.shallowred.Game;

import at.pwd.boardgame.game.mancala.agent.MancalaAgent;

/**
 * One instance of a mancalaAgentFactory always produces the same MancalaAgent
 */
public interface MancalaAgentFactory
{
    MancalaAgent produce();
}
