package shared.messages;

import java.lang.UnsupportedOperationException;

public class KVServerResponseMessage implements KVMessage {
    protected StatusType status;

    public KVServerResponseMessage(StatusType status){
        this.status = status;
    }

    public String getKey() {
        throw new UnsupportedOperationException("getKey");
    }
    
    public String getValue() {
        throw new UnsupportedOperationException("getValue");
    }

    public StatusType getStatus() {
        return this.status;
    }
}