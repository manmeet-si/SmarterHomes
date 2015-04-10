package com.assignment2.dos.smarterHomes;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/***
 * This class used to represents a clock and be able to change its time without modifying
 * the user system clock.
 */

public class BerkeleyClock {
	private static final TimeZone REF_TIME_ZONE = TimeZone.getTimeZone("UTC");
	private final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
	private long diffFromSysTime;
	private SimpleDateFormat dateFormat;
	public BerkeleyClock() {
		// As we start the clock, it is synchronized to the system time.
		diffFromSysTime = 0;
		dateFormat = new SimpleDateFormat(DATE_FORMAT);
	}
	
	/**
	 * Get the current time in long datatype format
	 */
	public long getTime() {
		return Calendar.getInstance(REF_TIME_ZONE).getTime().getTime() + diffFromSysTime;
	}
	
	/**
	 * Sets the current time
	 */

	public void setTime(long time) {
		diffFromSysTime = time - Calendar.getInstance(REF_TIME_ZONE).getTime().getTime();
	}
	
	/**
	 * Adds the milliseconds to the time difference from system-time 
	 */

	public void addMilliseconds(long milliseconds) {
		diffFromSysTime += milliseconds;
	}
	
	@Override
	public String toString() {
		return dateFormat.format(new Date(getTime()));
	}
}