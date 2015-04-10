package com.assignment2.dos.smarterHomes;
/**
 * @author Manmeet Singh
 * @email msingh@cs.umass.edu
 *
 */

/**
 * 
 * Sensors contains the basic functionality for Sensor: Takes the Sensor id and name as parameter
 *
 */
public class Sensors {
	int id;
	String name;
	
	public Sensors() {
		id = -1;
		name = null;
	}
	
	public Sensors(int id, String name) {
		this.id = id;
		this.name = name;
	}
	
	public int getId() {
		return id;
	}

	private void setId(int id) {
		this.id = id;
	}

		
	
}
