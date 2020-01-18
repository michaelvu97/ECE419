package shared.messages;

public abstract class KVClientRequestMessage implements KVMessage {
    @Override
    public StatusType getStatus(){
        throw new java.lang.UnsupportedOperationException("getStatus");
    }
}