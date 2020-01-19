
package storage;

public interface IFileStorage {

    /**
    * @return written key/value pair.
    */
    public String writeToFile(String key, String value);

    /**
    * @return deleted key/value pair.
    */
    public String deleteFromFile(String key, String value);

  	/**
    * @return value if found, otherwise return null.
    */
    public String readFromFile(String key);

    /**
    * @return success on file deletion.
    */
    public boolean deleteFile(String key);
}