package storage;
import shared.metadata.*;
import shared.*;

public interface IDiskStorage {

    /**
    * @return int 1 or 2.
    * 1: new entry inserted into file.
    * 2: existing entry updated.
    */
    public int put(String key, String value);

    /**
    * @return value if found, otherwise return null.
    */
    public String get(String key);

    /**
    * removes given key/value pair from file on disk.
    * @return true if the key existed and was successfully deleted
    */
    public boolean delete(String key);

    /**
    * removes all keys of a given hash range from storage
    * @return true if complted succesfully
    */
    public boolean flush(HashRange hr);
    /**
     * returns a key value pair within a hash range, null if none exist
     */
    public Pair popInRange(HashRange hr);

    /**
    * @return deletes file on disk.
    */
    public void clear();
}
