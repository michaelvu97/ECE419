package server;

import java.util.concurrent.locks.Lock;
import cache.*;
import storage.*;

public class ServerStoreSmart implements IServerStore {
    
    private Object _lock = new Object();

    private ICache _cache;

    private IDiskStorage _disk;

    public ServerStoreSmart(int cacheSize, String strategy){
        this._cache = new Cache(cacheSize, strategy); 
    }

    @Override    
    public String get(String key) {
        synchronized(_lock) {
            String value = _cache.get(key);
            if (value == null) {
                value = _disk.get(key);
                _cache.put(key, value);
            }
            return value;
        }
    }
    
    @Override        
    public IServerStore.PutResult put(String key, String value) {
        IServerStore.PutResult result = IServerStore.PutResult.FAILED;
        synchronized(_lock) {
            _cache.put(key, value);

            result = _disk.put(key, value) == 1 ? IServerStore.PutResult.INSERTED :
                IServerStore.PutResult.UPDATED;
                        
            return result;
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
}