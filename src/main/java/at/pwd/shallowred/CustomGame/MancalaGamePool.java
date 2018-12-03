package at.pwd.shallowred.CustomGame;

import at.pwd.shallowred.Utils.Pool;

public class MancalaGamePool extends Pool<MancalaGame>
{

    @Override
    protected MancalaGame instantiate()
    {
        return new MancalaGame();
    }
}
