package shared;

import java.math.BigInteger;

public final class HashRange {
    // TODO: unit tests.
    private BigInteger _hashStart;
    private BigInteger _hashEnd;

    private boolean _wrapsAround = false;

    /**
     * Constructs a new hash range.
     * @param start the inclusive start to the hash range
     * @param end the exclusive end to the hash range. May be less than start,
     * which implies that the hash range wraps around.
     */
    public HashRange(String start, String end) {
        if (start == null)
            throw new IllegalArgumentException("start");
        if (end == null)
            throw new IllegalArgumentException("end");
        if (start.length() != 32)
            throw new IllegalArgumentException("start");
        if (end.length() != 32)
            throw new IllegalArgumentException("end");

        _hashStart = new BigInteger(start.getBytes());
        _hashEnd = new BigInteger(end.getBytes());

        _wrapsAround = _hashStart.compareTo(_hashEnd) > 0;
    }

    /**
     * @return true if the given hash string is contained within this hash 
     * range.
     */
    public boolean IsInRange(String hashCheck) {
        if (hashCheck == null)
            throw new IllegalArgumentException("hashCheck");
        if (hashCheck.length() != 32)
            throw new IllegalArgumentException("hashCheck");

        BigInteger val = new BigInteger(hashCheck.getBytes());

        if (_wrapsAround) {
            return _hashStart.compareTo(val) <= 0 
                    || val.compareTo(_hashEnd) < 0;
        }

        return _hashStart.compareTo(val) <= 0 
                && val.compareTo(_hashEnd) < 0;
    }
}