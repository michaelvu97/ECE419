package storage;

import shared.metadata.*;
import shared.*;
import java.util.*;

public final class DiskStorageInMemory implements IDiskStorage {

    private Map<String, String> _data;

    public DiskStorageInMemory() {
        _data = new HashMap<String,String>();
    }

    @Override
    public int put(String key, String value) {
        String prevVal = _data.put(key, value);
        if (prevVal == null)
            return 1;
        return 2;
    }

    @Override
    public String get(String key) {
        return _data.get(key);
    }

    @Override
    public boolean delete(String key) {
        if (!_data.containsKey(key))
            return false;


        _data.remove(key);
        return true;
    }

    @Override
    public boolean flush(HashRange hr) {
        Map<String, String> newMap = new HashMap<String, String>();

        for (Map.Entry<String, String> kv : _data.entrySet()) {
            HashValue keyHash = HashUtil.ComputeHashFromKey(kv.getKey());
            if (!hr.isInRange(keyHash)) {
                newMap.put(kv.getKey(), kv.getValue());
            }
        }

        _data = newMap;
        return true;
    }

    @Override
    public Pair popInRange(HashRange hr) {
        throw new IllegalStateException("POP IN RANGE Deprecated");
    }

    @Override
    public List<Pair> getAllInRange(HashRange hr) {
        List<Pair> result = new ArrayList<Pair>();

        for (Map.Entry<String, String> kv : _data.entrySet()) {
            HashValue keyHash = HashUtil.ComputeHashFromKey(kv.getKey());
            if (hr.isInRange(keyHash)) {
                result.add(new Pair(kv.getKey(), kv.getValue()));
            }
        }

        return result;
    }

    @Override
    public void clear() {
        _data = new HashMap<String, String>();
    }
}