package cache;
import server.IServerStore;

public interface ICache {

	/**
     * Check if the cahce contains a given key. Return value.
     **/
	public String get(String key);

	/**
	* Insert new object into the cahce
	**/	
	public IServerStore.PutResult put(String key, String value);

	/**
	* Remove a key from the cahce, if it exists
	**/
	public void delete(String key);

	/**
	* Completely clear cache
	**/
	public void clear();
}