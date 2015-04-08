package com.assignment2.dos.smartHomes;
/**
 * @author Manmeet Singh
 * @email msingh@cs.umass.edu
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
