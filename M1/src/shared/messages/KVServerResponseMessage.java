package shared.messages;

import java.lang.UnsupportedOperationException;
import shared.Utils;
import shared.Serializer;
import shared.Deserializer;

public class KVServerResponseMessage extends KVMessageBase {

    public KVServerResponseMessage(StatusType status, String key, String value) {
        // Utils.validateResponseMessage(responseMessage);
        super(status, key, value);
    }

    public static KVServerResponseMessage GET_ERROR(String key) {
        return new KVServerResponseMessage(StatusType.GET_ERROR, key, null);
    }

    public static KVServerResponseMessage GET_SUCCESS(String key, String value) {
        return new KVServerResponseMessage(StatusType.GET_SUCCESS, key, value);
    }

    public static KVServerResponseMessage PUT_SUCCESS(String key, String value) {
        return new KVServerResponseMessage(StatusType.PUT_SUCCESS, key, value);
    }

    public static KVServerResponseMessage PUT_UPDATE(String key, String value) {
        return new KVServerResponseMessage(StatusType.PUT_UPDATE, key, value);
    }

    public static KVServerResponseMessage PUT_ERROR(String key, String value) {
        return new KVServerResponseMessage(StatusType.PUT_ERROR, key, value);
    }

    public static KVServerResponseMessage DELETE_SUCCESS(String key) {
        return new KVServerResponseMessage(StatusType.DELETE_SUCCESS, key, null);
    }

    public static KVServerResponseMessage DELETE_ERROR(String key) {
        return new KVServerResponseMessage(StatusType.DELETE_ERROR, key, null);
    }

    public static KVServerResponseMessage Deserialize(byte[] bytes)
            throws Deserializer.DeserializationException {
        return (KVServerResponseMessage) KVMessageBase.Deserialize(bytes);
    }

}