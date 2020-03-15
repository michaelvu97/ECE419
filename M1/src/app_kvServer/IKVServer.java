package app_kvServer;
import shared.metadata.*;
import shared.*;
import java.util.*;

public interface IKVServer {
    public enum CacheStrategy {
        None,
        LRU,
        LFU,
        FIFO
    };

    public enum ServerStateType {
        IDLE,
        STARTED,
        SHUT_DOWN,
        STOPPED // Default server status; server is stopped.
    };
    
    public String getName();

    /**
     * Get the port number of the server
     * @return  port number
     */
    public int getPort();

    /**
     * Get the hostname of the server
     * @return  hostname of server
     */
    public String getHostname();

    /**
     * Get the cache strategy of the server
     * @return  cache strategy
     */
    public CacheStrategy getCacheStrategy();

    /**
     * Get the cache size
     * @return  cache size
     */
    public int getCacheSize();

    /**
     * Check if key is in storage.
     * NOTE: does not modify any other properties
     * @return  true if key in storage, false otherwise
     */
    public boolean inStorage(String key);

    /**
     * Check if key is in storage.
     * NOTE: does not modify any other properties
     * @return  true if key in storage, false otherwise
     */
    public boolean inCache(String key);

    /**
     * Get the value associated with the key
     * @return  value associated with key
     * @throws Exception
     *      when key not in the key range of the server
     */
    public String getKV(String key) throws Exception;

    /**
     * Put the key-value pair into storage
     * @throws Exception
     *      when key not in the key range of the server
     */
    public void putKV(String key, String value) throws Exception;

    /**
     * Clear the local cache of the server
     */
    public void clearCache();

    /**
     * Clear the storage of the server
     */
    public void clearStorage();

    /**
     * Removes any entries from storage/cache that don't belong to the hash range.
     */
    public void refocus(HashRange hr);
    
    /**
     * Grabs a key value pair in a hash range, returns the values, and removes it from storage 
     */
    public Pair popInRange(HashRange hr);

    /**
    * Grabs a list of all key value pairs which are in a given hash range
    */
    public List<Pair> getAllInRange(HashRange hr);
    /**
     * Assumes that the server has already been write locked.
     * Sends all data that the new server is responsible for.
     */
    public boolean transferDataToServer(MetaData serverToSendTo);

    /**
     * Starts running the server
     */
    public void run();

    /**
     * Abruptly stop the server without any additional actions
     * NOTE: this includes performing saving to storage
     */
    public void kill();

    /**
     * Gracefully stop the server, can perform any additional actions
     */
    public void close();

    public ServerStateType getServerState();
    public void setServerState(ServerStateType state);

    public boolean isWriterLocked();

    public void writeLock();
    public void writeUnlock();

    public MetaData getMetaData();
}
