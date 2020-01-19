package shared.messages;

import shared.Deserializer;

public abstract class KVClientRequestMessage implements KVMessage {

    public enum RequestType {
        PUT((byte) 0),
        GET((byte) 1);
        
        public final byte val;

        public static RequestType FromByte(byte b){
            
            switch (b) {
                case 0:
                    return PUT;
                case 1:
                    return GET;
                default:                
                    throw new IllegalArgumentException("b out of range");
            }
        }

        private RequestType(byte val){
            this.val = val;
        }
    }

    private RequestType _type;

    public RequestType getType() {
        return this._type;
    }

    protected KVClientRequestMessage(RequestType type)
    {
        this._type = type;
    }

    /**
     * Decodes a client request object from a string, and creates a 
     * KVClientRequestMessage.
     */
    public static KVClientRequestMessage Deserialize(byte[] clientRequestBytes) {
        if (clientRequestBytes == null || clientRequestBytes.length == 0)
            throw new IllegalArgumentException("clientRequestBytes");

        Deserializer deserializer = new Deserializer(clientRequestBytes);

        // First byte is the method type
        RequestType requestType = RequestType.FromByte(deserializer.getByte());
        KVClientRequestMessage result = null;

        switch (requestType) {
            case GET:
                // Decode the key.
                result = new KVGetMessage(deserializer.getString());
                break;
            case PUT:
                // Decode the key and value
                result = new KVPutMessage(
                    deserializer.getString(), 
                    deserializer.getString()
                );
                break;
            default:
                throw new IllegalArgumentException("Invalid request type");
        }

        return result;
    }

    @Override
    public StatusType getStatus(){
        throw new java.lang.UnsupportedOperationException("getStatus");
    }
}