package com.assignment2.dos.smartHomes;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by idurugkar on 4/7/15.
 */

/**Database is a file containing the status of the sensors as a CSV
 *  The format of the CSV is:
 *  sensorName, status, time
 *
 *  This is the format in which the file is written
 *  But we are returning only the sensor name and status when read
 */

public class Database {

    private static final String databaseName = "statusDB.csv";
    private static int lock = 0;
    private CSVReader dbReader;
    private CSVWriter dbWriter;

    //Locks to ensure only one instance is accessing the database
    private static boolean checkLock()
    {
        return lock==0;
    }
    private static void getLock()
    {
        lock = 1;
    }
    private static void releaseLock()
    {
        lock = 0;
    }

    //Constructor
    Database()
    {
        File f = new File(databaseName);
        try {
            if (!f.exists())
                f.createNewFile();
        }
        catch (IOException E)
        {
            System.out.println("Couldn't create database file");
        }
    }

    //Put the given status of the provided sensor in the database
    public boolean PutStatus(String sensor, String status, String time)
    {
        try {
            while(checkLock());
            getLock();
            //Open the file to write
            dbWriter = new CSVWriter(new FileWriter(databaseName));
            String[] record = new String[3];
            record[0] = sensor;
            record[1] = status;
            record[2] = time;

            //Write record at the end
            dbWriter.writeNext(record);
            dbWriter.close();   //Close file for persistence
            releaseLock();
        }
        catch(FileNotFoundException E)
        {
            System.out.println("The Database file does not exist");
        }
        catch (IOException E)
        {
            System.out.println("IO Exception encountered");
        }
        return false;
    }

    //Put the given status of the provided sensor in the database without time
    //If no time provided, save as '--'
    public boolean PutStatus(String sensor, String status)
    {
        try {
            while(checkLock());
            getLock();
            //Open the file to write
            dbWriter = new CSVWriter(new FileWriter(databaseName));
            String[] record = new String[3];
            record[0] = sensor;
            record[1] = status;
            record[2] = "--";

            //Write record at the end
            dbWriter.writeNext(record);
            dbWriter.close();   //Close file for persistence
            releaseLock();
        }
        catch(FileNotFoundException E)
        {
            System.out.println("The Database file does not exist");
        }
        catch (IOException E)
        {
            System.out.println("IO Exception encountered");
        }
        return false;
    }

    // return status of all: devices and components
    public HashMap<String, List<String>> GetAll()
    {
        HashMap<String, List<String>> status = new HashMap<String, List<String>>();
        try {

            while(checkLock());
            getLock();
            //Open reader
            dbReader = new CSVReader(new FileReader(databaseName));
            String[] nextLine;
            //Read records one by one
            while ((nextLine = dbReader.readNext()) != null) {
                //Add record to HasMap
                List<String> s = new ArrayList();
                s.add(nextLine[1]);
                status.put(nextLine[0], s);
            }
            releaseLock();
            //Return HashMap with latest status of all sensors
            return status;
        }
        catch (FileNotFoundException E)
        {
            System.out.println("The Database file does not exist");
        }
        catch (IOException E)
        {
            System.out.println("IO Exception encountered");
        }
        if(!checkLock())
            releaseLock();
        return null;
    }


    //Method to get the complete history of a particular sensor
    public HashMap<String, List<String>> GetHistory(String sensor)
    {
        HashMap<String, List<String>> status = new HashMap<String, List<String>>();
        try {
            while(checkLock());
            getLock();

            //Open file for reading
            dbReader = new CSVReader(new FileReader(databaseName));
            String[] nextLine;

            //Read each record one by one
            while ((nextLine = dbReader.readNext()) != null) {
                //Add to list if it belongs to the sensor we want
                if(nextLine[0].equals(sensor)) {
                    List<String> s= new ArrayList<String>();
                    s.add(nextLine[1]);
                    // Append to list if not the first instance
                    if(status.containsKey(sensor))
                    {
                        s.addAll(status.get(sensor));
                        status.put(sensor,s);
                    }
                    else {
                        // Add new if first instance
                        status.put(sensor, s);
                    }
                }
            }
            releaseLock();
            return status;
        }
        catch (FileNotFoundException E)
        {
            System.out.println("The Database file does not exist");
        }
        catch (IOException E)
        {
            System.out.println("IO Exception encountered");
        }
        releaseLock();
        return null;
    }

    //Method to get the latest status of sensor
    public HashMap<String, List<String>> GetCurrentStatus(String sensorName)
    {
        HashMap<String, List<String>> status = new HashMap<String, List<String>>();
        try {
            while(checkLock());
            getLock();
            dbReader = new CSVReader(new FileReader(databaseName));
            String[] nextLine;
            while ((nextLine = dbReader.readNext()) != null) {
                if(nextLine[0].equals(sensorName)) {
                    List<String> s= new ArrayList<String>();
                    s.add(nextLine[1]);
                    status.put(sensorName, s);
                }
            }
            releaseLock();
            return status;
        }
        catch (FileNotFoundException E)
        {
            System.out.println("The Database file does not exist");
        }
        catch (IOException E)
        {
            System.out.println("IO Exception encountered");
        }
        releaseLock();
        return null;
    }


    //Method to get the complete history of all sensors
    public HashMap<String, List<String>> GetAllHistory()
    {
        HashMap<String, List<String>> status = new HashMap<String, List<String>>();
        try {
            while(checkLock());
            getLock();
            //Open reader
            dbReader = new CSVReader(new FileReader(databaseName));
            String[] nextLine;

            //read records one by one
            while ((nextLine = dbReader.readNext()) != null) {
                String sensor = nextLine[0];
                List<String> s= new ArrayList<String>();
                s.add(nextLine[1]);
                //If already added, append to history
                if(status.containsKey(sensor))
                {
                    s.addAll(status.get(sensor));
                    status.put(sensor,s);
                }
                //If new, add current instance
                else {
                    status.put(sensor, s);
                }

            }
            releaseLock();
            return status;
        }
        catch (FileNotFoundException E)
        {
            System.out.println("The Database file does not exist");
        }
        catch (IOException E)
        {
            System.out.println("IO Exception encountered");
        }
        releaseLock();
        return null;
    }

}
