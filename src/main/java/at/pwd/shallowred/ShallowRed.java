package at.pwd.shallowred;
import at.pwd.boardgame.game.mancala.MancalaGame;
import at.pwd.boardgame.game.mancala.agent.MancalaAgent;
import at.pwd.boardgame.game.mancala.agent.MancalaAgentAction;

import java.util.List;

public class ShallowRed implements MancalaAgent {
    @Override
    public MancalaAgentAction doTurn(int computationTime, MancalaGame mancalaGame) {
        // get a list of all currently selectable slots
        List<String> slots = mancalaGame.getSelectableSlots();
        // since this list will never be empty (otherwise the game would be over), we dont need a additional check
        // Slot IDs are unique strings strings
        String selectedSlot = slots.get(0);
        // now we pack the selected slot in an agent action and return it
        // the Mancala Boardgame Engine will then apply this action onto the slot
        return new MancalaAgentAction(selectedSlot);
    }

    @Override
    public String toString() {
        return "Choose First Agent";
    }
}
