package com.assignment2.dos.smarterHomes;

/**
 * Devices class contains the device id and the device name as parameters along with the status of the device.
 * Devices class is the super class for all the devices 
 * and contains the basic functionality which is inherited
 * by each device. Currently the LightBulb and the Outlet inherit from it
 */

public class Devices {
    int id;
    String name;

    State status;

    /**
     * Default Constructor
     */
    public Devices() {
        id = -1;
        name = null;
        status = State.OFF;
    }

    /**
     * Parameterized constructor for Devices class
     */
    public Devices(int id, String name) {
        this.id = id;
        this.name = name;
        status = State.OFF;
    }

    /**
     * method returning device id
     */
    public int getId() {
        return id;
    }

    /**
     * methos to set the device id
     */
    private void setId(int id) {
        this.id = id;
    }

    
    /**
     * Method to turn the device on
     */
    public void turnOn()    {this.status = State.ON; System.out.println("Turning device:" + this.status);}


    /**
     * Method to turn the device off
     */
    public void turnOff()   {this.status = State.OFF; System.out.println("Turning device" + this.status);}


    /**
     * Method to return the status of the device
     */
    public State getStatus()  {
        return this.status;
    }



}
