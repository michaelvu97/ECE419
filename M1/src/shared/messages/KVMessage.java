package shared.messages;

import shared.serialization.*;

public interface KVMessage extends ISerializable {
	
	public enum StatusType {
		GET, 			/* Get - request */
		GET_ERROR, 		/* requested tuple (i.e. value) not found */
		GET_SUCCESS, 	/* requested tuple (i.e. value) found */
		PUT, 			/* Put - request */
		PUT_SUCCESS, 	/* Put - request successful, tuple inserted */
		PUT_UPDATE, 	/* Put - request successful, i.e. value updated */
		PUT_ERROR, 		/* Put - request not successful */
		DELETE_SUCCESS, /* Delete - request successful */
		DELETE_ERROR, 	/* Delete - request successful */
		SERVER_STOPPED,          /* Server is stopped, no requests are processed */
  		SERVER_WRITE_LOCK,       /* Server locked for out, only get possible */
  		SERVER_NOT_RESPONSIBLE,  /* Request not successful, server not responsible for key */
  		GET_METADATA,            /* Pls give me the metadata - request */
  		GET_METADATA_SUCCESS,    /* Ok here you go, let me introduce you to my friends - request successful */
  		PUT_DUMP,				 /* Put request from one server to another, recieving server does not check for whether it is in the correct hash range*/
  		PUT_BACKUP,				 /* Put request from one server to another, recieving server checks if it lies in its backup range*/
  		INCORRECT_BACKUP_LOCATION;				

		public byte toByte() {
			switch (this) {
				case GET:
					return 0;
				case GET_ERROR:
					return 1;
				case GET_SUCCESS:
					return 2;
				case PUT:
					return 3;
				case PUT_SUCCESS:
					return 4;
				case PUT_UPDATE:
					return 5;
				case PUT_ERROR:
					return 6;
				case DELETE_SUCCESS:
					return 7;
				case DELETE_ERROR:
					return 8;
				case SERVER_STOPPED:
					return 9;
				case SERVER_WRITE_LOCK:
					return 10;
				case SERVER_NOT_RESPONSIBLE:
					return 11;
				case GET_METADATA:
					return 12;
				case GET_METADATA_SUCCESS:
					return 13;
				case PUT_DUMP:
					return 14;
				case PUT_BACKUP:
					return 15;
				case INCORRECT_BACKUP_LOCATION:
					return 16;
                default:                
                    throw new IllegalArgumentException("s out of range");
            }
		}

		public static StatusType FromByte(byte b) {
            switch (b) {
				case 0:
					return GET;
				case 1:
					return GET_ERROR;
				case 2:
					return GET_SUCCESS;
				case 3:
					return PUT;
				case 4:
					return PUT_SUCCESS;
				case 5:
					return PUT_UPDATE;
				case 6:
					return PUT_ERROR;
				case 7:
					return DELETE_SUCCESS;
				case 8:
					return DELETE_ERROR;
				case 9:
					return SERVER_STOPPED;
				case 10:
					return SERVER_WRITE_LOCK;
				case 11:
					return SERVER_NOT_RESPONSIBLE;
				case 12:
					return GET_METADATA;
				case 13:
					return GET_METADATA_SUCCESS;
				case 14:
					return PUT_DUMP;
				case 15:
					return PUT_BACKUP;
				case 16:
					return INCORRECT_BACKUP_LOCATION;
                default:                
                    throw new IllegalArgumentException("b out of range");
            }
        }
	}

	/**
	 * @return the key that is associated with this message, 
	 * 		null if not key is associated.
	 */
	public String getKey();
	
	/**
	 * @return the value that is associated with this message, 
	 * 		null if not value is associated.
	 */
	public String getValue();

	/**
	 * @return the value of this message as raw bytes.
	 */
	public byte[] getValueRaw();
	
	/**
	 * @return a status string that is used to identify request types, 
	 * response types and error types associated to the message.
	 */
	public StatusType getStatus();
}


