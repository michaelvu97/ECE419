package shared.messages;

import shared.serialization.*;

public final class KVMessageImpl implements KVMessage {

    private String _key = null;
    private byte[] _value = null;
    private KVMessage.StatusType _type;

    public KVMessageImpl(KVMessage.StatusType type, String key, String value) {
        this(type, key, value == null ? null : value.getBytes());
    }

    public KVMessageImpl(KVMessage.StatusType type, String key) {
        this(type, key, (byte[]) null);
    }

    public KVMessageImpl(KVMessage.StatusType type, String key, byte[] value) {
        _type = type;
        _key = key;
        _value = value;

        if (getStatus() == KVMessage.StatusType.GET) {
            if (getValue() != null)
                throw new IllegalArgumentException("Value cannot be set for GET requests");
            if (getKey() == null)
                throw new IllegalArgumentException("Key cannot be null for GET requests");
        }

        if (getStatus() == KVMessage.StatusType.PUT) {
            if (getKey() == null)
                throw new IllegalArgumentException("Key cannot be null for PUT requests");
        }

        // TODO?: checks for server reponses validity.
    }

    public String toString() {
        if (_value == null) {
            return getStatus().toString() + "<" + getKey() + ">";
        } else if (getStatus() == KVMessage.StatusType.SERVER_NOT_RESPONSIBLE
                || getStatus() == KVMessage.StatusType.SERVER_STOPPED) {
            return getStatus().toString() + "<" + getKey() + ",[server_metadata]>";
        } else {
            return getStatus().toString() + "<" + getKey() + "," + getValue() + ">";
        }

        
    }

    @Override
    public String getKey() {
        return _key;
    }

    @Override
    public String getValue() {
        return _value == null ? null : new String(_value);
    }

    @Override
    public byte[] getValueRaw() {
        return _value;
    }

    @Override
    public KVMessage.StatusType getStatus() {
        return _type;
    }

    @Override
    public byte[] serialize() {
        return 
            new Serializer()
            .writeByte(getStatus().toByte())
            .writeString(getKey())
            .writeBytes(getValueRaw())
            .toByteArray();
    }

    /**
     * Attempts to deserialize a byte stream.
     */
    public static KVMessage Deserialize(byte[] serializedBytes) 
            throws Deserializer.DeserializationException {
        Deserializer d = new Deserializer(serializedBytes);

        KVMessage.StatusType type = KVMessage.StatusType.FromByte(d.getByte());
        String key = d.getString();
        byte[] value = d.getBytes();

        return new KVMessageImpl(type, key, value);
    }
}