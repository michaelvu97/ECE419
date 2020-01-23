package cache;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

import app_kvServer.IKVServer;
import server.IServerStore;

public class Cache implements ICache {
	
	//stratagy store for replacement policy
	private IKVServer.CacheStrategy _strategy;

	//cache size store
	private int cacheSize;

	private class CacheEntry {
		public String key;
		public String value;
		public int num_used;
		public CacheEntry next;
		public CacheEntry previous;

		public CacheEntry (String newKey, String newValue){
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

	CacheEntry lastEntry = new CacheEntry(null,"last!");
	CacheEntry firstEntry = new CacheEntry(null,"first!");

	private Map<String, CacheEntry> _cacheMap = new HashMap<String, CacheEntry>();
	

	public Cache(int size, IKVServer.CacheStrategy strategy){
		//init cache array
		cacheSize = size;

		_strategy = strategy;
		//if(_strategy == IKVServer.CacheStrategy.LFU) System.out.println("LFULFULFULFULFU");;
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
			CacheEntry valueEntry = _cacheMap.get(key);
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
					CacheEntry toSwitch = valueEntry.next;
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
	public IServerStore.PutResult put(String key, String value){
		//if the key already exists on put, update value
		//System.out.println("inserting "+ key + " and " + value);
		if(_cacheMap.containsKey(key)){
			CacheEntry toModify = _cacheMap.get(key);
			if(value == toModify.value) return IServerStore.PutResult.IDENTICAL;
			toModify.value = value;
			return IServerStore.PutResult.UPDATED;
		} else {
			//check if a replacement is necassary
			if(_cacheMap.size() == cacheSize){
				
				//if it is find the first element and pop it out
				CacheEntry toRemove = firstEntry.next;
				//System.out.println(toRemove.key + " is being removed");

				toRemove.removeElement();
				//also remove it from the hash list
				//System.out.println("removing " + toRemove.key);

				_cacheMap.remove(toRemove.key);
			}
			//if it isnt already there, add it to the hash map and the linked list
			CacheEntry newEntry = new CacheEntry(key,value);
			//insert new entry at the back of the queue
			if(_strategy != IKVServer.CacheStrategy.LFU){
				newEntry.previous = lastEntry.previous;
				newEntry.next = lastEntry;
				lastEntry.previous = newEntry;
				newEntry.previous.next = newEntry;
			} else {
				CacheEntry insertIndex = firstEntry.next;
				//System.out.println("doing lfu insert starting with " + insertIndex.value);
				while (insertIndex.key != null && insertIndex.num_used == 0){
					//System.out.println("trying " + insertIndex.key);
					insertIndex = insertIndex.next;
				}
				newEntry.previous = insertIndex.previous;
				newEntry.next = insertIndex;
				newEntry.next.previous = newEntry;
				newEntry.previous.next = newEntry;
			}
			//System.out.println("first entry = " + firstEntry.next.key + " last entry = " + lastEntry.previous.key);
			//insert into the cache map
			_cacheMap.put(key,newEntry);
			return IServerStore.PutResult.INSERTED;

		}
	}

	public void delete(String key){
		if(_cacheMap.containsKey(key)){
			//remove element from linked list
			CacheEntry toRemove = _cacheMap.get(key);
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