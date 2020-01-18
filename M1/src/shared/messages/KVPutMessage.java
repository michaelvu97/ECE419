package shared.messages;

public final class KVPutMessage extends KVClientRequestMessage {

    private String _key;
    private String _value;

    // For now, public constructor
    public KVPutMessage(String key, String value) {
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
}