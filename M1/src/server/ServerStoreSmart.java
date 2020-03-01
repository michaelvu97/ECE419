package server;

import java.util.concurrent.locks.Lock;
import cache.*;
import storage.*;

import app_kvServer.IKVServer;
import shared.metadata.*;
import shared.*;

/**
 * Implementation of IServerStore. This class is not thread safe.
 */
public class ServerStoreSmart implements IServerStore {
    
    private Object _lock = new Object();

    private ICache _cache;
    private IDiskStorage _disk;

    public ServerStoreSmart(ICache cache, IDiskStorage diskStorage){
        if (cache == null)
            throw new IllegalArgumentException("Cache is null");
        if (diskStorage == null)
            throw new IllegalArgumentException("Disk storage is null");

        this._cache = cache;
        this._disk = diskStorage;
    }

    @Override    
    public String get(String key) {
        synchronized(_lock) {
            String value = _cache.get(key);
            if (value == null) {
                value = _disk.get(key);
                if(value != null){
                    _cache.put(key, value);
                }
            }
            return value;
        }
    }

    @Override    
    public String cacheGet(String key) {
        synchronized(_lock) {
            return _cache.get(key);
        }
    }
    
    @Override        
    public IServerStore.PutResult put(String key, String value) {
        if (value == null)
            throw new IllegalArgumentException("Value cannot be null");
        
        IServerStore.PutResult result = IServerStore.PutResult.FAILED;
        synchronized(_lock) {
            IServerStore.PutResult cacheResult = _cache.put(key, value);
            if(cacheResult != IServerStore.PutResult.IDENTICAL){
                result = _disk.put(key, value) == 1 ? IServerStore.PutResult.INSERTED :
                    IServerStore.PutResult.UPDATED;
                return result;
            } else {
                return IServerStore.PutResult.UPDATED;
            }
            
        }
    }
    
    @Override        
    public boolean delete(String key) {
        synchronized(_lock) {
            _cache.delete(key);
           return _disk.delete(key);
        }
    }
    
    @Override        
    public void clearCache() {
        synchronized(_lock) {
            _cache.clear();
        }
    }
    
    @Override        
    public void clearStorage() {
        synchronized(_lock) {
            _cache.clear();
            _disk.clear();
        }
    }
    
    @Override        
    public boolean inStorage(String key) {
        synchronized(_lock) {
            return _disk.get(key) == null ? false : true; 
        }
    }
    
    @Override        
    public boolean inCache(String key) {
        synchronized(_lock) {
            return _cache.get(key) == null ? false : true; 
        }
    }

    @Override
    public Pair popInRange(HashRange hr){
     Pair KV;
	 synchronized(_lock) {
	    KV = _disk.popInRange(hr);
	    if(KV!=null) _cache.delete(KV.k);	    
	}
    return KV;   	 
    }

    @Override
    public boolean flushStorage(HashRange hr){
        boolean return_val;
        synchronized(_lock) {
            clearCache();
            return_val = _disk.flush(hr);
        }
        return return_val;
    } 
}
