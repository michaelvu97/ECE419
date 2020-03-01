package server;
import shared.metadata.*;
import shared.*;
import java.util.*;
public interface IServerStore {

    /**
     * @return the result of the get operation. If the result is null, the key
     * was not present in the cache.
     */
    public String get(String key);
    
    public enum PutResult {
        INSERTED,
        UPDATED,
        FAILED,
        IDENTICAL
    }

    /**
     * @return operation success.
     */
    public PutResult put(String key, String value);

    /**
     * @return operation success.
     */
    public boolean delete(String key);

    public void clearCache();
    public void clearStorage();

    public boolean inStorage(String key);
    public boolean inCache(String key);

    public String cacheGet(String key);
    
    public Pair popInRange(HashRange hr);
    public List<Pair> getAllInRange(HashRange hr);
    /**
     * @return operation success.
     */
    public boolean flushStorage(HashRange hr);
}
