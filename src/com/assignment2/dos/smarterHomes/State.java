package com.assignment2.dos.smarterHomes;
/**
 * @author Manmeet Singh
 * @email msingh@cs.umass.edu
 *
 */

public enum State {
	OFF("off"), ON("on");
	String state;
	   
	State(String state) {
		this.state = state;;
	}
	   
	String getState() {
		return state;
	} 
}
