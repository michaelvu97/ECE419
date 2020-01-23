package server;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import cache.*;

import app_kvServer.IKVServer;

/**
 * A dumb, only in memory k-v store. Thread-safe (but not fast).
 */
public class ServerStoreDumb implements IServerStore {
    
    private Object _lock = new Object();

    private Map<String, String> _map = new HashMap<String, String>();

    private ICache _cache;

    public ServerStoreDumb(int cacheSize, IKVServer.CacheStrategy strategy){
        this._cache = new Cache(cacheSize, strategy); 
    }

    @Override    
    public String get(String key) {
        synchronized(_lock) {
            String value = _cache.get(key);
            if (value == null) value = _map.get(key);
            return value;
        }
    }
    
    @Override        
    public IServerStore.PutResult put(String key, String value) {
        IServerStore.PutResult result = IServerStore.PutResult.FAILED;
        synchronized(_lock) {
            if (_map.containsKey(key))
                result = IServerStore.PutResult.UPDATED;
            else
                result = IServerStore.PutResult.INSERTED;
            _map.put(key, value);
            _cache.put(key,value);
        }
        return result;
    }
    
    @Override        
    public boolean delete(String key) {
        synchronized(_lock) {
            if (_map.containsKey(key)) {
                _map.remove(key);
                return true;
            } else {
                return false;
            }
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
            _map.clear();
        }
    }
    
    @Override        
    public boolean inStorage(String key) {
        synchronized(_lock) {
            return _map.containsKey(key);
        }
    }
    
    @Override        
    public boolean inCache(String key) {
        synchronized(_lock) {
            if(_cache.get(key) == null) return false;
            else return true;
        }
    }
}