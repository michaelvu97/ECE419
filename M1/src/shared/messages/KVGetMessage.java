package shared.messages;

public final class KVGetMessage extends KVClientRequestMessage {
    private String _key;

    // For now, public constructor
    public KVGetMessage(String key){
        this._key = key;
    }

    @Override
    public String getKey() {
        return this._key;
    }

    @Override
    public String getValue() {
        throw new java.lang.UnsupportedOperationException("getValue");
    }
}