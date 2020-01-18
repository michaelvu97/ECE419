package client;

import shared.messages.*;
import java.lang.UnsupportedOperationException;

public class KVStore implements KVCommInterface {
	/**
	 * Initialize KVStore with address and port of KVServer
	 * @param address the address of the KVServer
	 * @param port the port of the KVServer
	 */
	public KVStore(String address, int port) {
		// throw new NotImplementedException("KVStore Constructor");
	}

	@Override
	public void connect() throws Exception {
		// TODO Auto-generated method stub
	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
	}

	@Override
	public KVMessage put(String key, String value) throws Exception {
		// TODO: validate input

		KVClientRequestMessage message = new KVPutMessage(key, value);
		KVServerResponseMessage response = send(message);

		return response;
	}

	@Override
	public KVMessage get(String key) throws Exception {
		// TODO: validate input

		KVClientRequestMessage message = new KVGetMessage(key);
		KVServerResponseMessage response = send(message);

		return response;
	}

	private KVServerResponseMessage send(KVClientRequestMessage requestMessage) throws Exception {
		throw new UnsupportedOperationException("send");
	}
}
