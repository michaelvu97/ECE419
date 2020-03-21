package client;

import shared.messages.*;
import shared.metadata.ServerInfo;

public class KVStoreTest extends KVStore {
	
	public KVStoreTest(ServerInfo entryPointServerInfo) {
		super(entryPointServerInfo);
	}

	public KVMessage get(String key, int replicaNum) {
		return null; // TODO
	}

}