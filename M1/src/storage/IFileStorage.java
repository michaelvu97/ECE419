package server;

public interface IFileStorage {
	
	/**
    * @return success on file creation.
    */
    public boolean createFile();

    /**
    * @return written key/value pair.
    */
    public String writeToFile(String key);

    /**
    * @return deleted key/value pair.
    */
    public String deleteFromFile(String key);

    /**
    * @return success on file deletion.
    */
    public boolean deleteFile(String key);
}