package com.assignment2.dos.smarterHomes;

/**
 * Devices class is the super class for all the devices 
 * and contains the basic functionality which is inherited
 * by each device
 */


public class Devices {
    int id;
    String name;

    State status;

    public Devices() {
        id = -1;
        name = null;
        status = State.OFF;
    }

    public Devices(int id, String name) {
        this.id = id;
        this.name = name;
        status = State.OFF;
    }

    public int getId() {
        return id;
    }

    private void setId(int id) {
        this.id = id;
    }

    public void turnOn()    {this.status = State.ON; System.out.println("Turning device:" + this.status);}

    public void turnOff()   {this.status = State.OFF; System.out.println("Turning device" + this.status);}

    public State getStatus()  {
        return this.status;
    }



}
