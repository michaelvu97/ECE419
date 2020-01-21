package shared.messages;

import shared.Deserializer;

public class KVClientRequestMessage extends KVMessageBase {

    protected KVClientRequestMessage(KVMessage.StatusType type, String key, String value)
    {
        super(type, key, value);

        // Validate type
        if (getStatus() != KVMessage.StatusType.PUT && getStatus() != KVMessage.StatusType.GET) {
            throw new IllegalArgumentException("type :" + type + " is not allowed");
        }

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
    }

    public static KVClientRequestMessage GET(String key) {
        return new KVClientRequestMessage(KVMessage.StatusType.GET, key, null);
    }

    public static KVClientRequestMessage PUT(String key, String value) {
        return new KVClientRequestMessage(KVMessage.StatusType.PUT, key, value);
    }

    public static KVClientRequestMessage Deserialize(byte[] bytes) throws
            Deserializer.DeserializationException {
        return (KVClientRequestMessage) KVMessageBase.Deserialize(bytes);
    }
}