package shared.metadata;

import java.math.BigInteger;
import java.util.Arrays;
import shared.serialization.*;


public final class HashRange implements ISerializable {
    // TODO: unit tests.
    private HashValue _hashStart;
    private HashValue _hashEnd;

    private boolean _wrapsAround;
    private boolean _encompassing;

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
        if (start.compareTo(end) == 0) {
            _encompassing = true;
            _wrapsAround = true;
        } else {
            _wrapsAround = _hashStart.compareTo(_hashEnd) > 0;
        }
    }

    /**
     * Returns true if two ranges are equal (but does NOT return true if they're
     * both encompassing).
     */
    public boolean equals(HashRange other) {
        if (other == null)
            return false;

        return _hashStart.compareTo(other._hashStart) == 0 &&
            _hashEnd.compareTo(other._hashEnd) == 0;
    }

    /**
     * @return true if the given hash string is contained within this hash 
     * range.
     */
    public boolean isInRange(HashValue hashCheck) {
        if (hashCheck == null)
            throw new IllegalArgumentException("hashCheck");

        if (_encompassing)
            return true;

        if (_wrapsAround) {
            return _hashStart.compareTo(hashCheck) <= 0 
                    || hashCheck.compareTo(_hashEnd) < 0;
        }

        return _hashStart.compareTo(hashCheck) <= 0 
                && hashCheck.compareTo(_hashEnd) < 0;
    }

    public HashValue getStart() {
        return _hashStart;
    }

    public HashValue getEnd() {
        return _hashEnd;
    }

    public boolean getWrapsAround() {
        return _wrapsAround;
    }

    public boolean getIsEncompassing() {
        return _encompassing;
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
        
        byte[] hashStartBytes = d.getObjectBytes();
        byte[] hashEndBytes = d.getObjectBytes();

        return new HashRange(
            HashValue.Deserialize(hashStartBytes), 
            HashValue.Deserialize(hashEndBytes)
        );
    }
}