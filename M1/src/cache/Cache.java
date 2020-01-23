package cache;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

import app_kvServer.IKVServer;

public class Cache implements ICache {
	
	//stratagy store for replacement policy
	private IKVServer.CacheStrategy _strategy;

	//cache size store
	private int cacheSize;

	private class cacheEntry {
		public String key;
		public String value;
		public int num_used;
		public cacheEntry next;
		public cacheEntry previous;

		public cacheEntry (String newKey, String newValue){
			key=newKey;
			value = newValue;
			num_used = 0;
			next = null;
			previous = null;
		}

		public void removeElement () {
			if(next != null){
				next.previous = previous;
			} 
			else{ 
				next.previous = null;
			}
			if(previous != null){
				previous.next = next;
			} else {
				previous.next = null;
			}
		}
	}

	cacheEntry lastEntry = new cacheEntry(null,null);
	cacheEntry firstEntry = new cacheEntry(null,null);

	private Map<String, cacheEntry> _cacheMap = new HashMap<String, cacheEntry>();
	

	public Cache(int size, IKVServer.CacheStrategy strategy){
		//init cache array
		cacheSize = size;

		_strategy = strategy;
		//System.out.println("strategy = "+ strategy + " (" + strategyString + ")");
		//point the header and footer of the linked list together
		lastEntry.previous = firstEntry;
		firstEntry.next = lastEntry;
	}
	
	/**
    * Check if the cahce contains a given key. Return value or null if it does not exist.
    **/
	public String get(String key){
		//System.out.println("trying to get "+ key);
		if(_cacheMap.containsKey(key)){
			cacheEntry valueEntry = _cacheMap.get(key);
			if(_strategy == IKVServer.CacheStrategy.LRU){
				//remove entry from linked list and insert it in the back of the list
				//System.out.println(valueEntry.key + " is being moved to the back");
				valueEntry.removeElement();
				valueEntry.previous = lastEntry.previous;
				valueEntry.next = lastEntry;
				lastEntry.previous.next = valueEntry;
				lastEntry.previous = valueEntry;
			} else if (_strategy == IKVServer.CacheStrategy.LFU){
				//increment and check whether to switch
				valueEntry.num_used++;
				while(valueEntry.next.key != null && valueEntry.num_used>valueEntry.next.num_used){
					//System.out.println(valueEntry.key + " is swapping up");
					//switch order based on frequency of use
					cacheEntry toSwitch = valueEntry.next;
					valueEntry.next = toSwitch.next;
					toSwitch.previous = valueEntry.previous;
					valueEntry.previous = toSwitch;
					toSwitch.next = valueEntry;
					toSwitch.previous.next = toSwitch;
					valueEntry.next.previous = valueEntry;
				}
			}
			//System.out.println("  pass! "+ key + " (" + valueEntry.value + ")");
			return valueEntry.value;
		} else {
			//System.out.println("  fail! "+ key);
			return null;
		}
	}

	/**
	* Insert new object into the cahce
	**/	
	public void put(String key, String value){
		//if the key already exists on put, update value
		//System.out.println("inserting "+ key + " and " + value);
		if(_cacheMap.containsKey(key)){
			cacheEntry toModify = _cacheMap.get(key);
			toModify.value = value;
		} else {
			//check if a replacement is necassary
			if(_cacheMap.size() == cacheSize){
				
				//if it is find the first element and pop it out
				cacheEntry toRemove = firstEntry.next;
				//System.out.println(toRemove.key + " is being removed");

				toRemove.removeElement();
				//also remove it from the hash list
				_cacheMap.remove(toRemove.key);
			}
			//if it isnt already there, add it to the hash map and the linked list
			cacheEntry newEntry = new cacheEntry(key,value);
			//insert new entry at the back of the queue
			if(_strategy != IKVServer.CacheStrategy.LFU){
				newEntry.previous = lastEntry.previous;
				newEntry.next = lastEntry;
				lastEntry.previous = newEntry;
				newEntry.previous.next = newEntry;
			} else {
				cacheEntry insertIndex = firstEntry.next;
				while (insertIndex.key != null && insertIndex.num_used != 0){
					insertIndex = insertIndex.next;
				}
				newEntry.previous = insertIndex.previous;
				newEntry.next = insertIndex;
				insertIndex.previous = insertIndex;
				newEntry.previous.next = newEntry;
			}
			//System.out.println("first entry = " + firstEntry.next.key);
			//insert into the cache map
			_cacheMap.put(key,newEntry);

		}
	}

	public void remove(String key){
		if(_cacheMap.containsKey(key)){
			//remove element from linked list
			cacheEntry toRemove = _cacheMap.get(key);
			toRemove.removeElement();
			//also remove it from the hash list
			_cacheMap.remove(key);
		}
		return;
	}

	public void clear(){
		//remove all elements from the linked list
		firstEntry.next = lastEntry;
		lastEntry.previous = firstEntry;
		_cacheMap.clear();
	}
}