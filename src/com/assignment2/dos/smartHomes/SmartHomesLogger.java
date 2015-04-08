package com.assignment2.dos.smartHomes;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

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

		   // System.out.println("Done");
		} catch(IOException e) {
		  //  System.out.println("COULD NOT LOG!!");
		}
		/*Logger logger = Logger.getLogger("MyLog");  
		FileHandler fh = null;  

		try {  

		    // This block configure the logger with handler and formatter  
		    fh = new FileHandler("Log.ini");  
		    logger.addHandler(fh);
		    SimpleFormatter formatter = new SimpleFormatter();  
		    fh.setFormatter(formatter);  

		    // the following statement is used to log any messages  
		    logger.info(log); 
			fh.close();

		} catch (SecurityException e) {  
		    e.printStackTrace();  
		} catch (IOException e) {  
		    e.printStackTrace();  
		} 
		*/ 	
	}

}



