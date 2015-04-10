/* @author Manmeet Singh
 * @email msingh@cs.umass.edu
 *
 */

package com.assignment2.dos.smarterHomes;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/*
 * This class calculates the current time
 * using the calendar class provided in java utils library
 * and return in Hours:Minute:Seconds format
 */

public class CurrentTime {

    public static String getCurrentTime() {
    	Calendar cal = Calendar.getInstance();
    	cal.getTime();
    	SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    	return ("" + sdf.format(cal.getTime()) );
    }

}