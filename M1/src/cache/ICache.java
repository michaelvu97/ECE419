package cache;

public interface ICache {

	/**
     * Check if the cahce contains a given key. Return value.
     **/
	public String get(String key);

	/**
	* Insert new object into the cahce
	**/	
	public void put(String key, String value);

	/**
	* Remove a key from the cahce, if it exists
	**/
	public void remove(String key);
}