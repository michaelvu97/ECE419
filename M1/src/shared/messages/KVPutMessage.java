package shared.messages;

import shared.Serializer;

public final class KVPutMessage extends KVClientRequestMessage {

    private String _key;
    private String _value;

    // For now, public constructor
    public KVPutMessage(String key, String value) {
        super(RequestType.PUT);
        this._key = key;
        this._value = value;
    }

    @Override
    public String getKey() {
        return this._key;
    }

    @Override
    public String getValue() {
        return this._value;
    }

    @Override
    public byte[] convertToBytes(){
        Serializer serializer = new Serializer();

        serializer
            .writeByte(getType().val)
            .writeString(getKey())
            .writeString(getValue());

        return serializer.toByteArray();
    }
}