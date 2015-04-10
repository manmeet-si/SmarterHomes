/** @author Manmeet Singh
 * @email msingh@cs.umass.edu
 *
 */

package com.assignment2.dos.smarterHomes;

/***
 * Created by idurugkar on 4/8/15.
 */

/**
 * This class implements the LogicalClock functionality
 */
public class LogicalClock {
    private double time;

    LogicalClock()
    {
        this.time = 0;
    }

    /**
     * On event it increases the logical clock
     */
    public void Event()
    {
            time++;
    }

    /**
     * This method compares the logical clock and updates it
     */
    public double Compare(Double otherTime)
    {
        time = Math.max(time, otherTime) + 1;
        return time;
    }

    /**
     * Returns the current logical clock value
     */
    public double GetTime()
    {
        return time;
    }
    /**
     * Returns the current logical clock value as String Data-Type
     */
    public String GetStringTime()
    { return Double.toString(time);}
}
