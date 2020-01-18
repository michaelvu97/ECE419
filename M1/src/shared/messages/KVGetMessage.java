package shared.messages;

import shared.Serializer;

public final class KVGetMessage extends KVClientRequestMessage {
    private String _key;

    // For now, public constructor
    public KVGetMessage(String key) {
        super(RequestType.GET);
        this._key = key;
    }

    @Override
    public String getKey() {
        return this._key;
    }

    @Override
    public String getValue() {
        throw new java.lang.UnsupportedOperationException("getStatus");
    }

    @Override
    public byte[] convertToBytes(){
        Serializer serializer = new Serializer();

        serializer
            .writeByte(getType().val)
            .writeString(getKey());

        return serializer.toByteArray();
    }
}