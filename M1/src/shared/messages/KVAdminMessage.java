package shared.messages;

import shared.serialization.*;

/**
 * Used for communicating between KVServer and ECS
 */
public class KVAdminMessage implements ISerializable {

    public enum StatusType {
        TODO;

        public byte toByte() {
            switch (this) {
                case TODO:
                    return -1;
                default:                
                    throw new IllegalArgumentException("s out of range");
            }
        }

        public static StatusType FromByte(byte b) {
            switch (b) {
                case -1:
                    return TODO;
                default:                
                    throw new IllegalArgumentException("b out of range");
            }
        }
    }

    private StatusType _status;
    protected byte[] payloadBytes = null;

    public KVAdminMessage(StatusType status, byte[] payloadBytes) {
        // Will throw an error if the status is invalid.
        StatusType.FromByte(status.toByte());
        
        _status = status;
        this.payloadBytes = payloadBytes;
    }


    /**
     * @return a status string that is used to identify request types, 
     * response types and error types associated to the message.
     */
    public StatusType getStatus() {
        return _status;
    }

    /**
     * @return the value of this message as raw bytes.
     */
    public byte[] getPayload() {
        return payloadBytes;
    }

    public byte[] serialize() {
        return new Serializer()
            .writeByte(getStatus().toByte())
            .writeBytes(getPayload())
            .toByteArray();
    }

    public static KVAdminMessage Deserialize(byte[] serializedBytes) 
            throws Deserializer.DeserializationException {
        if (serializedBytes == null)
            throw new IllegalArgumentException("serializedBytes is null");

        Deserializer d = new Deserializer(serializedBytes);

        return new KVAdminMessage(
            StatusType.FromByte(d.getByte()),
            d.getBytes()
        );
    }
}