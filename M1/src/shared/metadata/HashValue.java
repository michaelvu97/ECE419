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

        // Correct the byte array.
        if (bytes.length < 16) {
            byte[] tempBytes = new byte[16];
            int diff = 16 - bytes.length;
            System.arraycopy(bytes, 0, tempBytes, diff, bytes.length);
            bytes = tempBytes;
            for (int i = 0; i < diff; i++) {
                bytes[i] = 0;
            }
        }
        if (bytes.length > 16) {
            byte[] tempBytes = new byte[16];
            System.arraycopy(bytes, bytes.length - 16, tempBytes, 0, 16);
            bytes = tempBytes;
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
                    (byte) Integer.parseInt(md5HashString.substring(i, i + 2), 16);
        }
        return new HashValue(bytes);
    }
}