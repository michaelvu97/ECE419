package shared.metadata;

import java.math.BigInteger;
import shared.serialization.*;

/**
 * Wrapper class for MD5 hash values.
 */
public final class HashValue implements ISerializable {

    private BigInteger _val;

    private HashValue(byte[] bytes) {
        if (bytes == null)
            throw new IllegalArgumentException("bytes is null");
        if (bytes.length != 16) {
            throw new IllegalArgumentException("bytes is invalid length : " 
                + bytes.length);
        }

        _val = new BigInteger(1, bytes);
    }

    public int compareTo(HashValue rhs) {
        return _val.compareTo(rhs._val);
    }

    @Override
    public byte[] serialize() {
        byte[] bytes = _val.toByteArray();
        if (bytes.length != 16) {
            throw new IllegalStateException("bytes not the right length: " 
                + bytes.length);
        }

        return new Serializer()
            .writeBytes(bytes)
            .toByteArray();
    }

    public static HashValue Deserialize(byte[] serializedBytes) 
            throws Deserializer.DeserializationException {
        return new HashValue(new Deserializer(serializedBytes).getBytes());
    }

    public static HashValue CreateFromHashString(String md5HashString) {
        if (md5HashString == null)
            throw new IllegalArgumentException("md5HashString is null");
        if (md5HashString.length() != 32) {
            throw new IllegalArgumentException(
                "md5HashString is illegal length: " + md5HashString.length());
        }

        // Create byte from every bi-gram.
        byte[] bytes = new byte[16];
        for (int i = 0; i < 32; i += 2) {
            bytes[i / 2] = 
                    Byte.parseByte(md5HashString.substring(i, i + 2), 16);
        }
        return new HashValue(bytes);
    }
}