package at.pwd.shallowred.Game;

import at.pwd.boardgame.game.agent.AgentAction;
import at.pwd.boardgame.game.base.WinState;
import at.pwd.boardgame.game.mancala.MancalaGame;
import at.pwd.boardgame.game.mancala.agent.MancalaAgent;
import at.pwd.boardgame.game.mancala.agent.MancalaAgentAction;
import at.pwd.shallowred.CustomGame.MancalaBoard;

import java.io.IOException;
import java.io.Writer;

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

    private Writer log;

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

    private void reset(MancalaAgent agentA, MancalaAgent agentB, int computingTime)
    {
        //TODO set game
        MancalaBoard defaultBoard = new MancalaBoard(6);
        reset(agentA,agentB,computingTime,defaultBoard.toMancalaGame());
    }

    private void reset(MancalaAgent agentA, MancalaAgent agentB, int computingTime, MancalaGame initialGame)
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

    public Writer getLogWriter()
    {
        return log;
    }

    public void setLogWriter(Writer logWriter)
    {
        this.log = logWriter;
    }

    @Override
    public void run()
    {
        try
        {
            logState();

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

                //log
                logState();
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

            //log result
            try
            {
                if(log!=null)
                {
                    if(result==NOBODY)
                        log.write("Nobody won due to an error");
                    else if(result==DRAW)
                        log.write("Draw");
                    else
                    {
                        MancalaBoard board = new MancalaBoard(game);
                        int diff = Math.abs(board.getFields()[MancalaBoard.DEPOT_A]-board.getFields()[MancalaBoard.DEPOT_B]);
                        log.write(at.pwd.shallowred.CustomGame.MancalaGame.playerIdToString(result) + ", " + (result == 0 ? agentA.toString() : agentB.toString()) + ", has won with a difference of " + diff + " stones");
                    }
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    private void logState() throws IOException
    {
        if(log==null)
            return;

        at.pwd.shallowred.CustomGame.MancalaGame mg = new at.pwd.shallowred.CustomGame.MancalaGame(game);

        if(mg.getWinner() == at.pwd.shallowred.CustomGame.MancalaGame.NOBODY)
        {
            log.write("+-----------------------------------------------------+" + System.lineSeparator());
            log.write(String.format("|%10s | %40s|", String.format("TURN:%3d", turns + 1), current.toString() + " is thinking.") + System.lineSeparator());
        }
        log.write(mg.toString()+System.lineSeparator());
        log.write(System.lineSeparator());
        //log.write(current.toString()+" chooses action "+action.);
    }
}
