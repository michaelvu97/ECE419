package cache;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

public class Cache implements ICache {
	
	//stratagy store for replacement policy
	private int strategy;
	//cache size store
	private int cacheSize;
	//cache slots filled store
	private int cacheSlotsFilled = 0;

	private Map<String, String> _cacheMap = new HashMap<String, String>();
	private LinkedList<String> replacementList = new LinkedList<String>(); 

	public Cache(int size, String strategyString){
		//init cache array
		cacheSize = size;

		//set strategy of cache
		if(strategyString.toLowerCase() == "fifo"){
			strategy = 0;
		}
		else if(strategyString.toLowerCase() == "lru"){
			strategy = 1;
		}
		else if(strategyString.toLowerCase() == "lfu"){
			strategy = 2;
		}
	}

	private void removeElementFromList (String key){
		int removeIndex = replacementList.indexOf(key);
		replacementList.remove(removeIndex);
	}
	
	/**
    * Check if the cahce contains a given key. Return value or null if it does not exist.
    **/
	public String get(String key){
		if(_cacheMap.containsKey(key)){
			if(strategy == 1){
				//if we are using lru, put the element at the back of the list on get to indicate it has been used
				removeElementFromList(key);
				replacementList.addLast(key);
			}
			return _cacheMap.get(key);
		} else {
			return null;
		}
	}

	/**
	* Insert new object into the cahce
	**/	
	public void put(String key, String value){
		//if the key already exists on put, update value or delete if value == null
		if(_cacheMap.containsKey(key)){
			_cacheMap.replace(key,value);
		} else {
			//check if a replacement is necassary
			if(_cacheMap.size() == cacheSize){
				//grab first element of the list and remove it from the hash and the list
				String toRemove = replacementList.pop();
				_cacheMap.remove(toRemove);

			}
			//if it isnt already there, add it to the hash map and the linked list
			_cacheMap.put(key,value);
			replacementList.addLast(key);

		}
	}

	public void remove(String key){
		if(_cacheMap.containsKey(key)){
			_cacheMap.remove(key);
			removeElementFromList(key);

		}
	}
}