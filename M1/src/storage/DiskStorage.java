package storage;

import java.util.*;
import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import shared.metadata.*;
import org.apache.log4j.Logger;
import shared.*;


/**
 * Implementation of IDiskStorage. This class is not thread safe.
 */
public class DiskStorage implements IDiskStorage {
	
    private Logger logger = Logger.getRootLogger();
	private static String BASE_PATH = System.getProperty("user.home") + "/M1/src/storage/";

    private String _id;
    private String _storagePath;

    private static boolean ALLOW_DEFAULT = true;

    public DiskStorage(String id) {
        if (id == null || id.length() == 0) {
            if (ALLOW_DEFAULT) {
                _id = "DEFAULT";
            } else {
                throw new IllegalArgumentException("id is null, and ALLOW_DEFAULT is not enabled");
            }
        } else {
            _id = id;
        }

        _storagePath = BASE_PATH + _id + ".diskstorage";
    }

    private void createNewFile() {
        File file = new File(_storagePath);
        try {
            if (!file.exists()){
                logger.debug("Creating disk file at: " + _storagePath);
                file.createNewFile();
            }
        } catch (IOException ioe) {
            logger.error("Could not create new disk storage file.", ioe);
        }
    }

    private void correctnessCheck() {
        int lineNum = 0; 

        /**
         * Check that the file has an even number of lines.
         * If not, insert one new line to the end of the file.
         */
        try { 
            BufferedReader br = new BufferedReader(new FileReader(_storagePath));
            
            while(br.readLine() != null) {
                lineNum++;
            }

            if(lineNum % 2 != 0) {
                FileWriter fw = new FileWriter(_storagePath);
                fw.write(System.getProperty("line.separator"));
            }
        } catch (Exception e) {
            logger.error("could not conduct disk file correctness check.");
        }
    }

	private int findEntryLine(String key) {
		String line;
		int lineNum = 1;
		int lineRet = 0;
    	    		
    	try { 
    		BufferedReader br = new BufferedReader(new FileReader(_storagePath));
    		while((line = br.readLine()) != null) {
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

    public List<Pair> getAllInRange(HashRange hr){
        String currLine = null;
        List<Pair> KVlist = new ArrayList<Pair>(); 
        createNewFile();
        correctnessCheck();

        /**
         * Create a new temp storage file
         * Copy all existing file except the K/V to be deleted.
         * Delete old storage file, rename new file to old file name.
         */
        try {
            // TODO: close the reader/writers using 'finally'
            BufferedReader br = new BufferedReader(new FileReader(_storagePath));
            StringBuilder sb = new StringBuilder();

            while((currLine = br.readLine()) != null) {
                if (!(hr.isInRange(HashUtil.ComputeHashFromKey(currLine)))) {
                    sb.append(currLine).append("\n");
                    currLine = br.readLine();
                    sb.append(currLine).append("\n");
                } else {
                    sb.append(currLine).append("\n");
                    String valLine = br.readLine();
                    sb.append(valLine).append("\n");

                    Pair KV = new Pair(currLine,valLine);
                    KVlist.add(KV);
                }
            }
            br.close();

            FileWriter fw = new FileWriter(_storagePath);
            fw.write(sb.toString());
            fw.close();
        } 
        catch (FileNotFoundException fnfe) {
            logger.error("Could not find file on disk.");
            return KVlist;
        } 
        catch (IOException ioe) {
            logger.error("Could not complete removal");
            return KVlist;
        }
        return KVlist;
    }

    @Override
    public Pair popInRange(HashRange hr){
        String currLine = null;
        boolean read_complete = false;
        Pair KV  = new Pair(null,null);	
        createNewFile();
        correctnessCheck();

        /**
         * Create a new temp storage file
         * Copy all existing file except the K/V to be deleted.
         * Delete old storage file, rename new file to old file name.
         */
        try {
            // TODO: close the reader/writers using 'finally'
            BufferedReader br = new BufferedReader(new FileReader(_storagePath));
            StringBuilder sb = new StringBuilder();

            while((currLine = br.readLine()) != null) {
                if (!(hr.isInRange(HashUtil.ComputeHashFromKey(currLine))) || read_complete) {
                    sb.append(currLine).append("\n");
                    currLine = br.readLine();
                    sb.append(currLine).append("\n");
                } else {
		            KV.k = currLine;		
                    currLine = br.readLine(); //read val
		            KV.v = currLine;
		            read_complete = true;
                }
            }
            br.close();

            FileWriter fw = new FileWriter(_storagePath);
            fw.write(sb.toString());
            fw.close();
        } 
        catch (FileNotFoundException fnfe) {
            logger.error("Could not find file on disk.");
            return KV;
        } 
        catch (IOException ioe) {
            logger.error("Could not complete removal");
            return KV;
        }
        return KV;
    }	    
    
    @Override
    public int put(String key, String value) {    	    	
        int opType = 0;
        int lineNum = 0;

        createNewFile();
        correctnessCheck();

        lineNum = findEntryLine(key);

        // CASE 1: replace a line with the new value.
    	if (lineNum > 0) { 
            opType = 2;
    		Path path = Paths.get(_storagePath);
    		
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
                BufferedWriter bw = new BufferedWriter(new FileWriter(_storagePath, true));
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
    public String get(String key) {
        int lineNum = 1;
        String line;
        String value = null;

        createNewFile();
        correctnessCheck();
            
        try {
            BufferedReader br = new BufferedReader(new FileReader(_storagePath));
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
    public boolean delete(String key) {
        String currLine = null;

        createNewFile();
        correctnessCheck();

        boolean keyFound = false;

        /**
         * Create a new temp storage file
         * Copy all existing file except the K/V to be deleted.
         * Delete old storage file, rename new file to old file name.
         */
    	try {
            // TODO: close the reader/writers using 'finally'
	    	BufferedReader br = new BufferedReader(new FileReader(_storagePath));
	    	StringBuilder sb = new StringBuilder();

	    	while((currLine = br.readLine()) != null) {
	    		if (!currLine.equals(key)) {
	    			sb.append(currLine).append("\n");
                    currLine = br.readLine();
                    sb.append(currLine).append("\n");
	    		} else {
                    keyFound = true;
                    br.readLine(); // Skip line
                }
	    	}
            br.close();

            FileWriter fw = new FileWriter(_storagePath);
            fw.write(sb.toString());
            fw.close();
    	} 
        catch (FileNotFoundException fnfe) {
            logger.error("Could not find file on disk.");
            return false;
    	} 
        catch (IOException ioe) {
            logger.error("Could not delete entry of key " + key + ".");
            return false;
    	}
        return keyFound;
    }

    @Override
    public boolean flush(HashRange hr) {
        String currLine = null;

        createNewFile();
        correctnessCheck();

        /**
         * Create a new temp storage file
         * Copy all existing file except the K/V to be deleted.
         * Delete old storage file, rename new file to old file name.
         */
        try {
            // TODO: close the reader/writers using 'finally'
            BufferedReader br = new BufferedReader(new FileReader(_storagePath));
            StringBuilder sb = new StringBuilder();

            while((currLine = br.readLine()) != null) {
                if (!(hr.isInRange(HashUtil.ComputeHashFromKey(currLine)))) {
                    sb.append(currLine).append("\n");
                    currLine = br.readLine();
                    sb.append(currLine).append("\n");
                } else {
                    br.readLine(); // Skip line
                }
            }
            br.close();

            FileWriter fw = new FileWriter(_storagePath);
            fw.write(sb.toString());
            fw.close();
        } 
        catch (FileNotFoundException fnfe) {
            logger.error("Could not find file on disk.");
            return false;
        } 
        catch (IOException ioe) {
            logger.error("Could not delete all entries in hash range");
            return false;
        }
        return true;
    }

    @Override
    public void clear() {
        File file = new File(_storagePath);
        if (file.exists()) {
            file.delete();
        }  
    }
}
