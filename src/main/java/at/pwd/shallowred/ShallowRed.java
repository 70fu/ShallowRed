package at.pwd.shallowred;

import at.pwd.boardgame.game.base.WinState;
import at.pwd.boardgame.game.mancala.agent.MancalaAgent;
import at.pwd.boardgame.game.mancala.agent.MancalaAgentAction;
import at.pwd.shallowred.CustomGame.MancalaBoard;
import at.pwd.shallowred.CustomGame.MancalaGame;
import at.pwd.shallowred.CustomGame.MancalaGamePool;
import at.pwd.shallowred.Utils.Pool;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ShallowRed implements MancalaAgent {
    private MancalaGamePool gamePool = new MancalaGamePool();
    private MCTSTreePool nodePool = new MCTSTreePool();
    private Random r = new Random();
    private static final double C = 1.0f/Math.sqrt(2.0f);

    public class MCTSTreePool extends Pool<MCTSTree>
    {
        @Override
        protected MCTSTree instantiate()
        {
            return new MCTSTree();
        }
    }

    private class MCTSTree {
        private int visitCount;
        private int winCount;

        private MancalaGame game;
        private ShallowRed.MCTSTree parent;
        private List<ShallowRed.MCTSTree> children = new ArrayList<>(6);
        int actionId;

        MCTSTree()
        {
        }

        MCTSTree set(MancalaGame game)
        {
            this.game = game;
            return this;
        }

        boolean isNonTerminal() {
            return game.getWinner()==MancalaGame.NOBODY;
        }

        ShallowRed.MCTSTree getBestNode() {
            ShallowRed.MCTSTree best = null;
            double value = 0;
            for (ShallowRed.MCTSTree m : children) {
                double wC = (double)m.winCount;
                double vC = (double)m.visitCount;
                double currentValue =  wC/vC + C*Math.sqrt(2*Math.log(visitCount) / vC);


                if (best == null || currentValue > value) {
                    value = currentValue;
                    best = m;
                }
            }

            return best;
        }

        boolean isFullyExpanded() {
            return children.size() == game.getSelectableCount();
        }

        ShallowRed.MCTSTree move(int actionId) {
            //obtain a copy
            MancalaGame newGame = gamePool.obtain().copy(this.game);
            //perform turn
            newGame.performTurn(actionId);

            ShallowRed.MCTSTree tree = nodePool.obtain().set(newGame);
            tree.actionId = actionId;
            tree.parent = this;

            this.children.add(tree);

            return tree;
        }
    }

    @Override
    public MancalaAgentAction doTurn(int computationTime, at.pwd.boardgame.game.mancala.MancalaGame game) {
        long start = System.currentTimeMillis();

        //create root of monte carlo tree, convert MancalaGame from framework to our Mancala game
        ShallowRed.MCTSTree root = nodePool.obtain().set(new MancalaGame(game));

        while ((System.currentTimeMillis() - start) < (computationTime*1000 - 100)) {
            ShallowRed.MCTSTree best = treePolicy(root);
            int winner = defaultPolicy(best.game);
            backup(best, winner);
        }

        ShallowRed.MCTSTree selected = root.getBestNode();
        System.out.println("Selected action " + selected.winCount + " / " + selected.visitCount);
        return new MancalaAgentAction(selected.action);//TODO
    }

    private void backup(ShallowRed.MCTSTree current, int winner) {
        int hasWon = (winner== MancalaBoard.PLAYER_A)?1:0;

        while (current != null) {
            // always increase visit count
            current.visitCount++;

            // if it ended in a win => increase the win count
            current.winCount += hasWon;

            current = current.parent;
        }
    }

    private ShallowRed.MCTSTree treePolicy(ShallowRed.MCTSTree current) {
        while (current.isNonTerminal()) {
            if (!current.isFullyExpanded()) {
                return expand(current);
            } else {
                current = current.getBestNode();
            }
        }
        return current;
    }

    private ShallowRed.MCTSTree expand(ShallowRed.MCTSTree best) {
        return best.move(SelectionUtils.selectExpand(best.game));
    }

    private int defaultPolicy(MancalaGame game) {
        game = gamePool.obtain().copy(game); // copy original game

        int turnId;
        while(game.getWinner()==MancalaGame.NOBODY)
        {
            turnId = SelectionUtils.selectMove(game);
            game.performTurn(turnId);
        }

        //free game used for playthrough
        gamePool.free(game);

        return game.getWinner();
    }

    @Override
    public String toString() {
        return "ShallowRed";
    }
}
