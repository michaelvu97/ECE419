package shared.messages;

import shared.Deserializer;
import shared.Serializer;

public class KVMessageImpl implements KVMessage {

    private String _key = null;
    private String _value = null;
    private KVMessage.StatusType _type;

    public KVMessageImpl(KVMessage.StatusType type, String key, String value) {
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
            if (getValue() == null)
                throw new IllegalArgumentException("Value cannot be null for PUT requests");
        }

        // TODO?: checks for server reponses validity.
    }

    public String toString() {
        if (_value == null)
            return getStatus().toString() + "<" + getKey() + ">";
        else
            return getStatus().toString() + "<" + getKey() + "," + getValue() + ">";
    }

    @Override
    public String getKey() {
        return _key;
    }

    @Override
    public String getValue() {
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
            .writeString(getValue())
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
        String value = d.getString();

        return new KVMessageImpl(type, key, value);
    }
}