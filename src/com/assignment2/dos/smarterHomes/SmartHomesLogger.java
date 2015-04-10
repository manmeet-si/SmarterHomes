package com.assignment2.dos.smarterHomes;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Logger file to generate logs
 */
public class SmartHomesLogger {

	public SmartHomesLogger(String _log) {
		File log = new File("LOG.ini");

		try{
		    if(!log.exists()){
		       // System.out.println("We had to make a new file.");
		        log.createNewFile();
		    }

		    FileWriter fileWriter = new FileWriter(log, true);

		    BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
		    bufferedWriter.write(_log + "\n");
		    bufferedWriter.close();
		    fileWriter.close();

		   // System.out.println("Done");
		} catch(IOException e) {
		  //  System.out.println("COULD NOT LOG!!");
		}
		
		File outputFile = new File("test-output.txt");

		try{
		    if(!outputFile.exists()){
		       // System.out.println("We had to make a new file.");
		    	outputFile.createNewFile();
		    }

		    FileWriter fileWriter1 = new FileWriter(outputFile, true);

		    BufferedWriter bufferedWriter = new BufferedWriter(fileWriter1);
		    bufferedWriter.write(_log + "\n");
		    bufferedWriter.close();
		    fileWriter1.close();

		   // System.out.println("Done");
		} catch(IOException e) {
		  //  System.out.println("COULD NOT LOG!!");
		}	
	}	

}



