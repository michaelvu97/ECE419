package server;

public interface IServerStore {

    /**
     * @return the result of the get operation. If the result is null, the key
     * was not present in the cache.
     */
    public String get(String key);
    
    public enum PutResult {
        INSERTED,
        UPDATED,
        FAILED
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
}