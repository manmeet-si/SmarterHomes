package com.assignment2.dos.smarterHomes;
/**
 * @author Manmeet Singh
 * @email msingh@cs.umass.edu
 *
 */
/*
 * State is an enum type which keeps the info as On or Off for the devices
 * 
 */
public enum State {
	OFF("off"), ON("on");
	String state;
	   
	State(String state) {
		this.state = state;
	}
	  
	/*
	 * Returns the state of the device
	 */
	
	String getState() {
		return state;
	} 
}
