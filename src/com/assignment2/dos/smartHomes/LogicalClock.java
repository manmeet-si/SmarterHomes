package com.assignment2.dos.smartHomes;

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
        if(time<otherTime)
            time = otherTime+1;
        else
            time++;
        return time;
    }

    public long GetTime()
    {
        return time;
    }
}
