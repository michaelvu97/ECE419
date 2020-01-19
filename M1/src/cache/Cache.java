package cache;

public class Cache implements ICache {
	
	//stratagy store for replacement policy
	private int strategy;
	//cache size store
	private int cacheSize;
	//cache slots filled store
	private int cacheSlotsFilled = 0;

	//fifo specific index for deciding where to replace
	private int fifoReplace = 0;

	private class CacheElement {
		public int cacheValue;
		public String key;
		public String value;
	}
	private CacheElement[] cacheArray; 

	public Cache(int size, String strategyString){
		//init cache array
		cacheArray = new CacheElement[cacheSize];
		cacheSize = size;
		for(int i = 0; i<cacheSize; i++){
			cacheArray[i].cacheValue = 0;
			cacheArray[i].key = null;
			cacheArray[i].value = null;
		}

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
	
	/**
    * Check if the cahce contains a given key. Return value or null if it does not exist.
    **/
	public String get(String key){
		for(int i = 0; i<cacheSize; i++){
			//check for matching key, return value
			if(key == cacheArray[i].key){
				//lru
				if(strategy == 1){
					for(int j = 0; j < cacheSize; j++){
						if(cacheArray[i].cacheValue < cacheArray[j].cacheValue){
							cacheArray[j].cacheValue++;
						}
					}
					cacheArray[i].cacheValue = 1;
				//lfu
				} else if (strategy == 2){
					cacheArray[i].cacheValue++;
				}

				return cacheArray[i].value;
			}
		}
		return null;
	}

	/**
	* Insert new object into the cahce
	**/	
	public void put(String key, String value){
		//check if the key already exists in cache
		for(int i = 0; i<cacheSize; i++){
			if(key == cacheArray[i].key){
				return;
			}
		}
		//if the cache is not yet full, insert into the first slot		
		if(cacheSlotsFilled<cacheSize){
			//one more cahe slot filled
			cacheSlotsFilled++;
			//insert into the first open slot
			for(int i = 0; i<cacheSize; i++){
				if(cacheArray[i].key == null){
					cacheArray[i].key = key;
					cacheArray[i].value = value;
					if(strategy == 1){
						cacheArray[i].cacheValue=i+1;
					}
					return;
				}
			}
		} else {
			//replacement policy shiz
			//fifo
			if(strategy == 0) {
				cacheArray[fifoReplace].key = key;
				cacheArray[fifoReplace].value = value;
				fifoReplace++;
			//lru
			} else if (strategy == 1) {
				for(int i = 0; i<cacheSize; i++){
					if(cacheArray[i].cacheValue == cacheSize){
						cacheArray[i].key = key;
						cacheArray[i].value = value;
						cacheArray[i].cacheValue = 1;
					} else {
						cacheArray[i].cacheValue++;
					}  
				}
			//lfu
			} else if (strategy == 2) {
				int least_used_index = 0;
				//find lowest frequency
				for(int i = 1; i<cacheSize; i++){
					if(cacheArray[least_used_index].cacheValue > cacheArray[i].cacheValue){
						least_used_index = i;
					}
				}
				cacheArray[least_used_index].key = key;
				cacheArray[least_used_index].value = value;
				cacheArray[least_used_index].cacheValue = 0;
			}

		}

	return;
	}
}