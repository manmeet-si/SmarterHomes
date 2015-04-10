package com.assignment2.dos.smarterHomes;

/**
 * Created by idurugkar on 4/8/15.
 */
public class LogicalClock {
    private long time;

    LogicalClock()
    {
        this.time = 0;
    }

    public void Event()
    {
            time++;
    }

    public long Compare(long otherTime)
    {
        time = Math.max(time, otherTime) + 1;
        return time;
    }

    public long GetTime()
    {
        return time;
    }
    public String GetStringTime()
    { return Long.toString(time);}
}
