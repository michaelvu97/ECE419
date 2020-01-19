package shared.messages;

import java.lang.UnsupportedOperationException;
import shared.Utils;
import shared.Serializer;
import shared.Deserializer;

public class KVServerResponseMessage implements KVMessage {
    protected StatusType status;

    private String _responseMessage;

    public KVServerResponseMessage(StatusType status, String responseMessage){
        Utils.validateResponseMessage(responseMessage);
        this.status = status;
        this._responseMessage = responseMessage;
    }

    @Override
    public String getKey() {
        throw new UnsupportedOperationException("getKey");
    }
    
    @Override
    public String getValue() {
        throw new UnsupportedOperationException("getValue");
    }

    @Override
    public StatusType getStatus() {
        return this.status;
    }

    public String getResponseMessage() {
        return this._responseMessage;
    }

    @Override
    public byte[] convertToBytes() {
        return new Serializer()
            .writeByte(this.status.toByte())
            .writeString(getResponseMessage())
            .toByteArray();
    }

    public static KVServerResponseMessage Deserialize(byte[] bytes) {
        if (bytes == null || bytes.length == 0)
            throw new IllegalArgumentException("bytes is empty or null");

        Deserializer d = new Deserializer(bytes);

        return new KVServerResponseMessage(
            StatusType.FromByte(d.getByte()),
            d.getString()
        );
    }
}