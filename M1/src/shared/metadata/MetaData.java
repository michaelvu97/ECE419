package shared.metadata;

import java.util.Comparator;
import java.util.Collection;

import app_kvServer.IKVServer;
import shared.serialization.*;

public final class MetaData implements ISerializable, Comparable<MetaData> {
    
    private String _name;
    private String _host;
    private int _port;
    private HashRange _hashRange;

    public MetaData(String name, String host, int port, String startHash,
        String endHash) {
        this(
            name,
            host,
            port,
            new HashRange(
                HashValue.CreateFromHashString(startHash), 
                HashValue.CreateFromHashString(endHash)
            )
        );
    }

    private MetaData(String name, String host, int port, HashRange hashRange) {
        if (name == null)
            throw new IllegalArgumentException("name");
        if (host == null)
            throw new IllegalArgumentException("host");
        if (port < 0)
            throw new IllegalArgumentException("port");
        if (hashRange == null)
            throw new IllegalArgumentException("hashRange is null");
        _name = name;
        _host = host;
        _port = port;
        _hashRange = hashRange;
    }

    public String getName() {
        return _name;
    }

    public String getHost() {
        return _host;
    }

    public int getPort() {
        return _port;
    }

    public HashRange getHashRange() {
        return _hashRange;
    }

    public IKVServer.ServerStateType getServerStateType() {
        return null; // TODO
    }

    @Override
    public byte[] serialize() {
        Serializer s = new Serializer();
        s.writeString(_name)
            .writeString(_host)
            .writeInt(_port)
            .writeBytes(_hashRange.serialize());

        return s.toByteArray();
    }


    // Thanks a lot, java 1.7
    public static final Deserializer.ObjectDeserializer<MetaData> Deserialize = 
            new Deserializer.ObjectDeserializer<MetaData>() {
                @Override
                public MetaData deserialize(byte[] bytes) 
                        throws Deserializer.DeserializationException{
                    return Deserialize(bytes);
                }
            };

    public static MetaData Deserialize(byte[] serializedBytes) 
            throws Deserializer.DeserializationException {
        Deserializer d = new Deserializer(serializedBytes);

        return new MetaData(
            d.getString(),
            d.getString(),
            d.getInt(),
            HashRange.Deserialize(d.getBytes())
        );
    }

    /**
     * Sorter for start hash. Does not consider wrapping around.
     */    
    @Override
    public int compareTo(MetaData other) {
        return getHashRange().getStart().compareTo(
            other.getHashRange().getStart()
        );
    }
}