package shared.metadata;

import java.math.BigInteger;
import shared.serialization.*;

public final class HashRange implements ISerializable {
    // TODO: unit tests.
    private HashValue _hashStart;
    private HashValue _hashEnd;

    private boolean _wrapsAround = false;

    /**
     * Constructs a new hash range.
     * @param start the inclusive start to the hash range
     * @param end the exclusive end to the hash range. May be less than start,
     * which implies that the hash range wraps around.
     */
    public HashRange(HashValue start, HashValue end) {
        if (start == null)
            throw new IllegalArgumentException("start is null");
        if (end == null)
            throw new IllegalArgumentException("end is null");

        _hashStart = start;
        _hashEnd = end;
        _wrapsAround = _hashStart.compareTo(_hashEnd) > 0;
    }

    /**
     * @return true if the given hash string is contained within this hash 
     * range.
     */
    public boolean IsInRange(HashValue hashCheck) {
        if (hashCheck == null)
            throw new IllegalArgumentException("hashCheck");

        if (_wrapsAround) {
            return _hashStart.compareTo(hashCheck) <= 0 
                    || hashCheck.compareTo(_hashEnd) < 0;
        }

        return _hashStart.compareTo(hashCheck) <= 0 
                && hashCheck.compareTo(_hashEnd) < 0;
    }

    @Override
    public byte[] serialize() {
        return new Serializer()
            .writeObject(_hashStart)
            .writeObject(_hashEnd)
            .toByteArray();
    }

    public static HashRange Deserialize(byte[] serializedBytes) 
            throws Deserializer.DeserializationException {
        Deserializer d = new Deserializer(serializedBytes);
        
        byte[] hashStartBytes = d.getBytes();
        byte[] hashEndBytes = d.getBytes();

        if (hashStartBytes.length != 16)
            throw new IllegalArgumentException("hashStartBytes");
        if (hashEndBytes.length != 16)
            throw new IllegalArgumentException("hashEndBytes");

        return new HashRange(
            HashValue.Deserialize(hashStartBytes), 
            HashValue.Deserialize(hashEndBytes)
        );
    }
}