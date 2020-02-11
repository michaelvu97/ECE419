package shared;

import app_kvServer.IKVServer;
import shared.serialization.*;

public final class MetaData implements ISerializable {
    
    private String _name;
    private String _host;
    private int _port;
    private HashRange _hashRange;

    public MetaData(String name, String host, int port, String startHash,
        String endHash) {
        if (name == null)
            throw new IllegalArgumentException("name");
        if (host == null)
            throw new IllegalArgumentException("host");
        if (port < 0)
            throw new IllegalArgumentException("port");
        _name = name;
        _host = host;
        _port = port;
        _hashRange = new HashRange(startHash, endHash);
    }

    public String getName() {
        return _name;
    }

    public String getHost() {
        return _host;
    }

    public HashRange getHashRange() {
        return _hashRange;
    }

    public IKVServer.ServerStateType getServerStateType() {
        return null; // TODO
    }

    @Override
    public byte[] serialize() {
        return null; // TODO
    }
}