package storage;

import java.io.*;
import java.nio.file.*;

import org.apache.log4j.Logger;


/**
 * get/put requests for on-file key/value pairs
 */
public class FileStorage implements IFileStorage {
	
	String path = "user.dir/disk_storage.txt";
	String file = "disk_storage.txt";
 	
 	private Logger logger = Logger.getRootLogger();

	public FileStorage(){
	}

    /**
    * @return written key/value pair.
    */
    @Override
    public String writeToFile(String key, String value) {
    	try {
    		FileReader fr = new FileReader(path + file);
		} catch (FileNotFoundException fnfe) {
			logger.error("disk_storage.txt not found.");
		}


    	return "";
    }

    /**
    * @return deleted key/value pair.
    */
    @Override
    public String deleteFromFile(String key, String value) {
    	return "";
    }

    /**
    * @return value if found, otherwise return null.
    */
    @Override
    public String readFromFile(String key) {
    	try (BufferedReader br = new BufferedReader(new FileReader(path))) {
    		String value = null;
    		String line;
    		int lineNum = 0;
    		
    		while ((line = br.readLine()) != null) {
    			lineNum++;
    			if(lineNum % 2 != 0 && line.equals(key)) {
    				value = br.readLine(); // read next line for val associated with key
    				break;
    			}
    		}
    	} catch (Exception e) {
    		logger.error("Could not read from file.");
    	}
    	return value;
    }

    /**
    * @return success on file deletion.
    */
    @Override
    public boolean deleteFile(String key) {
		return true;
    }
}
