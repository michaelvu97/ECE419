package shared.messages;

import shared.Serializer;
import shared.Deserializer;

public abstract class KVMessageBase implements KVMessage {

    private String _key = null;
    private String _value = null;
    private KVMessage.StatusType _type;

    protected KVMessageBase(KVMessage.StatusType type, String key, String value) {
        _type = type;
        _key = key;
        _value = value;
        // Errors are not checked here, please override in implementation.
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

    public byte[] serialize() {
        return 
            new Serializer()
            .writeByte(getStatus().toByte())
            .writeString(getKey())
            .writeString(getValue())
            .toByteArray();
    }

    public static KVMessage Deserialize(byte[] serializedBytes) {
        Deserializer d = new Deserializer(serializedBytes);

        KVMessage.StatusType type = KVMessage.StatusType.FromByte(d.getByte());
        String key = d.getString();
        String value = d.getString();

        if (type == KVMessage.StatusType.PUT) {
            return KVClientRequestMessage.PUT(key, value);
        }
        if (type == KVMessage.StatusType.GET) {
            return KVClientRequestMessage.GET(key);
        }

        return new KVServerResponseMessage(type, key, value);
    }
}