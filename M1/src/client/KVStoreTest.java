package client;

import java.io.IOException;

import shared.messages.*;
import shared.metadata.ServerInfo;
import shared.serialization.Deserializer;
import shared.Utils;

public class KVStoreTest extends KVStore {
	
	public KVStoreTest(ServerInfo entryPointServerInfo) {
		super(entryPointServerInfo);
	}

	/**
	 * replicaNum: 0 for primary, 1 or 2 for replica.
	 */
	public KVMessage get(String key, int replicaNum) throws IOException, 
			Deserializer.DeserializationException{
		validateConnected();

		if (replicaNum < 0 || 2 < replicaNum) {
			throw new IllegalArgumentException("ReplicaNum is out of range: " +
					replicaNum);
		}

		try {
			Utils.validateKey(key);

			KVMessage message = new KVMessageImpl(
				KVMessage.StatusType.GET,
				key
			);

			KVMessage response = _serverCommManager.sendRequest(message, 
					replicaNum);

			return response;
		}
		catch (IOException ioe){
			logger.error("GET failed I/O", ioe);
			throw ioe;
		}
		catch (Deserializer.DeserializationException dse) {
			logger.error("GET failed, invalid server response", dse);
			throw dse;
		}
	}

}