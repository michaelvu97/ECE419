package shared.messages;

import shared.serialization.*;

/**
 * Used for communicating between KVServer and ECS
 */
public class KVAdminMessage implements ISerializable {

    public enum StatusType {
        TRANSFER_REQUEST, /* ECS -> KV, tells KV to transfer data to other server */
        TRANSFER_REQUEST_SUCCESS,
        TRANSFER_REQUEST_FAILURE,
        UPDATE_METADATA_REQUEST, /* ECS -> KV, tells KV the new metadata*/
        UPDATE_METADATA_REQUEST_SUCCESS,
        UPDATE_METADATA_REQUEST_FAILURE,
        KYS; /* Kills the KVServer */

        public byte toByte() {
            switch (this) {
                case TRANSFER_REQUEST:
                    return 0;
                case TRANSFER_REQUEST_SUCCESS:
                    return 1;
                case UPDATE_METADATA_REQUEST:
                    return 2;
                case UPDATE_METADATA_REQUEST_SUCCESS:
                    return 3;
                case UPDATE_METADATA_REQUEST_FAILURE:
                    return 4;
                case TRANSFER_REQUEST_FAILURE:
                    return 5;
                case KYS:
                    return 6;
                default:                
                    throw new IllegalArgumentException("s out of range");
            }
        }

        public static StatusType FromByte(byte b) {
            switch (b) {
                case 0:
                    return TRANSFER_REQUEST;
                case 1:
                    return TRANSFER_REQUEST_SUCCESS;
                case 2:
                    return UPDATE_METADATA_REQUEST;
                case 3:
                    return UPDATE_METADATA_REQUEST_SUCCESS;
                case 4:
                    return UPDATE_METADATA_REQUEST_FAILURE;
                case 5:
                    return TRANSFER_REQUEST_FAILURE;
                case 6:
                    return KYS;
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