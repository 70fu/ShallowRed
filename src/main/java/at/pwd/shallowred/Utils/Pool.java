package at.pwd.shallowred.Utils;

import java.util.ArrayList;

public abstract class Pool<T>
{
    private ArrayList<T> free;

    public Pool()
    {
        this(16);
    }

    public Pool(int capacity)
    {
        free = new ArrayList<>(capacity);
    }

    protected abstract T instantiate();

    public T obtain()
    {
        if(free.size()==0)
            return instantiate();
        else
            return free.remove(free.size()-1);
    }

    public void free(T o)
    {
        free.add(o);
    }



}
