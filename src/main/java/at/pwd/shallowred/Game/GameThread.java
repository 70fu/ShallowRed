package at.pwd.shallowred.Game;

import at.pwd.boardgame.game.agent.AgentAction;
import at.pwd.boardgame.game.base.WinState;
import at.pwd.boardgame.game.mancala.MancalaGame;
import at.pwd.boardgame.game.mancala.agent.MancalaAgent;
import at.pwd.boardgame.game.mancala.agent.MancalaAgentAction;
import at.pwd.boardgame.services.XSLTService;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import static at.pwd.shallowred.CustomGame.MancalaGame.DRAW;
import static at.pwd.shallowred.CustomGame.MancalaGame.NOBODY;

/**
 * Executes a game between two agent and stores the result
 * Agent a begins
 */
public class GameThread extends Thread
{
    private MancalaAgent agentA;
    private MancalaAgent agentB;
    private int computingTime;//in s

    private MancalaAgent current;
    private MancalaAgent waiting;

    private MancalaGame game;

    //GAME INFORMATION
    /**
     * -1=NOBODY
     * 0=PLAYER_A
     * 1=PLAYER_B
     * 2=DRAW
     */
    private int result;
    private int turns;

    public GameThread()
    {

    }

    public GameThread(MancalaAgent agentA, MancalaAgent agentB, int computingTime)
    {
        reset(agentA,agentB,computingTime);
    }

    public GameThread(MancalaAgent agentA, MancalaAgent agentB, int computingTime, MancalaGame initialGame)
    {
        reset(agentA,agentB,computingTime,initialGame);
    }

    public void reset(MancalaAgent agentA, MancalaAgent agentB, int computingTime)
    {
        game = new MancalaGame();
        game.loadBoard(generateBoard());
        game.nextPlayer();//init first player
        reset(agentA,agentB,computingTime,game);
    }

    public void reset(MancalaAgent agentA, MancalaAgent agentB, int computingTime, MancalaGame initialGame)
    {
        if(isAlive())
        {
            System.out.println("Game is running, please do not reset");
            return;
        }

        this.agentA = agentA;
        this.agentB = agentB;
        this.computingTime = computingTime;
        this.game = initialGame;

        current = agentA;
        waiting = agentB;

        result = NOBODY;
        turns = 0;
    }

    public int getResult()
    {
        return result;
    }

    public int getTurns()
    {
        return turns;
    }

    @Override
    public void run()
    {
        try
        {
            while (!isInterrupted() && game.checkIfPlayerWins().getState() == WinState.States.NOBODY)
            {
                //let agent think
                MancalaAgentAction action = current.doTurn(computingTime, game);

                //apply action
                AgentAction.NextAction nextAction = action.applyAction(game);

                //who is the next agent
                if (nextAction == AgentAction.NextAction.NEXT_PLAYER)
                {
                    //switch to other agent
                    MancalaAgent tmp = current;
                    current = waiting;
                    waiting = tmp;

                    //tell game to switch player
                    game.nextPlayer();
                }

                ++turns;
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            //set result
            WinState winState = game.checkIfPlayerWins();
            if(winState.getState()==WinState.States.NOBODY)
                result=NOBODY;
            else if (winState.getState()== WinState.States.MULTIPLE)
                result=DRAW;
            else
                result=winState.getPlayerId();
        }
    }

    //copied from SetUpController.java
    private static final String BOARD_GENERATOR_TRANSFORMER = "/board_generator.xsl";
    private InputStream generateBoard() {
        Map<String, String> params = new HashMap<>();
        params.put("num_stones", "6");
        params.put("slots_per_player", "6");
        params.put("computation_time", String.valueOf(computingTime));

        return XSLTService.getInstance().execute(
                BOARD_GENERATOR_TRANSFORMER,
                new StreamSource(new StringReader("<empty/>")),
                params
        );
    }
}
