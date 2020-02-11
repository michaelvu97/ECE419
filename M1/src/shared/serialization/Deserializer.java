package shared.serialization;

public class Deserializer {

    public class DeserializationException extends Exception {
        public DeserializationException(String exceptionStr) {
            super(exceptionStr);
        }
    }

    private byte[] _arr;

    private int _position = 0;

    private int totalLength() {
        return this._arr.length;
    }

    public Deserializer(byte[] byteArray) {
        if (byteArray == null)
            throw new IllegalArgumentException("byteArray is null");

        this._arr = byteArray;
    }

    private int remainingBytes() {
        return totalLength() - this._position;
    }

    public int getInt() throws DeserializationException {
        if (remainingBytes() < 4)
            throw new DeserializationException("Reached end of deserializer array");

        int result = 
            ((_arr[_position] & 0xFF) << 24)
            | ((_arr[_position + 1] & 0xFF) << 16)
            | ((_arr[_position + 2] & 0xFF) << 8)
            | ((_arr[_position + 3] & 0xFF));

        _position += 4;

        return result;
    }

    public byte getByte() throws DeserializationException {
        if (remainingBytes() <= 0)
            throw new DeserializationException("reached end of deserializer");
        return _arr[_position++];
    }

    public byte[] getBytes() throws DeserializationException {
        int length = getInt();
        if (length == 0)
            return null;

        if (remainingBytes() < length)
            throw new DeserializationException("Reached end of deserializer");

        // Yes I know there's a faster way of doing this.
        byte[] res = new byte[length];
        for (int i = 0; i < length; i++) {
            res[i] = _arr[_position + i];
        }

        return res;
    }

    public String getString() throws DeserializationException {
        // Read int
        int length = getInt();

        if (length == 0)
            return null;

        if (remainingBytes() < length)
            throw new DeserializationException("Reached end of deserializer");

        byte[] strBytes = new byte[length];

        for (int i = 0; i < length; i++) {
            strBytes[i] = _arr[_position + i];
        }

        _position += length;

        return new String(strBytes);
    }
}