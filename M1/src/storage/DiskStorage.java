package storage;

import java.util.*;
import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;

import org.apache.log4j.Logger;

public class DiskStorage implements IDiskStorage {
	
    private Logger logger = Logger.getRootLogger();
	private String _path = "./src/storage/disk_storage.txt";
    private String _tempFilePath = "./src/storage/temp_storage.txt";

    private void createNewFile() {
        File file = new File(_path);
        try {
            if (!file.exists()){
                file.createNewFile();
            }
        } catch (IOException ioe) {
            logger.error("Could not create new disk storage file.");
        }
    }

	private int findEntryLine(String key) {
		String line;
		int lineNum = 1;
		int lineRet = 0;
    	    		
    	try { 
    		BufferedReader br = new BufferedReader(new FileReader(_path));
    		while ((line = br.readLine()) != null) {
    			if(lineNum % 2 != 0 && line.equals(key)) {
    				lineRet = lineNum;
    				break;
    			}
    			lineNum++;
    		}
    	} catch (Exception e) {
    		logger.error("find entry: Could not read from file.");
    	}
    	return lineRet;
	}
   
    @Override
    public int writeToFile(String key, String value) {    	    	
        int opType = 0;
        int lineNum = 0;

        createNewFile();

        lineNum = findEntryLine(key);

        // CASE 1: replace a line with the new value.
    	if (lineNum > 0) { 
            opType = 2;
    		Path path = Paths.get(_path);
    		
            try {
    			List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
    			lines.set(lineNum, value);	
    			Files.write(path, lines, StandardCharsets.UTF_8);
    		} 
            catch (IOException ioe) {
    			logger.error("Could not replace value in file at key " + key + ".");
    		}
    	} 
        // CASE 2: add a new entry to the end of the file.
    	else { 
            opType = 1;

			try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(_path, true));
                bw.write(key);
                bw.newLine();
                bw.write(value);
                bw.newLine();
                bw.close();
	    	} catch (IOException ioe) {
    			logger.error("Could not add (" + key + ", " + value + " to file on disk.");
	    	}
    	}
    	return opType;
    }

    @Override
    public String readFromFile(String key) {
        int lineNum = 1;
        String line;
        String value = null;

        createNewFile();
            
        try {
            BufferedReader br = new BufferedReader(new FileReader(_path));
            while ((line = br.readLine()) != null) {
                if(lineNum % 2 != 0 && line.equals(key)) {
                    value = br.readLine();
                    break;
                }
                lineNum++;
            }
        } catch (Exception e) {
            logger.error("Could not read from file.");
        }
        return value;
    }

    @Override
    public void deleteFromFile(String key) {
        int lineNum = 1;
        String currLine;
    	File tempFile = new File(_tempFilePath);

        createNewFile();

    	try {
	    	BufferedReader br = new BufferedReader(new FileReader(_path));
	    	BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile));

	    	while((currLine = br.readLine()) != null) {
	    		if (!currLine.equals(key)) {
	    			bw.write(currLine);
                    bw.newLine();
	    		} else {
                    currLine = br.readLine(); // sketchy.
                }
	    		lineNum++;
	    	} 
	    	bw.close();
	    	br.close();

	    	// delete old file.
	    	Path oldDiskStorageFile = Paths.get(_path);
	    	Files.delete(oldDiskStorageFile);
	    	// rename new file to disk_storage.
	    	File newName = new File(_path);
	    	tempFile.renameTo(newName);

    	} 
        catch (FileNotFoundException fnfe) {
            logger.error("Could not find file on disk.");
    	} 
        catch (IOException ioe) {
            logger.error("Could not delete entry of key " + key + ".");
    	}
    }

    @Override
    public void deleteFile() {
        File file = new File(_path);
        if (file.exists()) {
            file.delete();
        }  
    }
}
