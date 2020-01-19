package server;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;

/**
 * A dumb, only in memory k-v store. Thread-safe (but not fast).
 */
public class ServerStoreDumb implements IServerStore {
    
    private Object _lock = new Object();

    private Map<String, String> _map = new HashMap<String, String>();

    @Override    
    public String get(String key) {
        synchronized(_lock) {
            return _map.get(key);
        }
    }
    
    @Override        
    public boolean put(String key, String value) {
        synchronized(_lock) {
            _map.put(key, value);
        }
        return true;
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
        // No-op
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
        return false;
    }
}