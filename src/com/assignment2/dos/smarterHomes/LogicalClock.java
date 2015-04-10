/* @author Manmeet Singh
 * @email msingh@cs.umass.edu
 *
 */

package com.assignment2.dos.smarterHomes;

/**
 * Created by idurugkar on 4/8/15.
 */

/*
 * This class implements the LogicalClock functionality
 */
public class LogicalClock {
    private long time;

    LogicalClock()
    {
        this.time = 0;
    }

    /*
     * On event it increases the logical clock
     */
    public void Event()
    {
            time++;
    }

    /*
     * This method compares the logical clock and updates it
     */
    public long Compare(long otherTime)
    {
        time = Math.max(time, otherTime) + 1;
        return time;
    }

    /*
     * Returns the current logical clock value
     */
    public long GetTime()
    {
        return time;
    }
    /*
     * Returns the current logical clock value as String Data-Type
     */
    public String GetStringTime()
    { return Long.toString(time);}
}
