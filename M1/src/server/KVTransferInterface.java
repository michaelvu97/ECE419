package server;

import shared.messages.KVMessage;

public interface KVTransferInterface {

	/**
	 * Establishes a connection to the storage service, i.e., to an arbitrary
	 * instance of the storage servers that makes up the storage service.
	 * @throws Exception if connection could not be established.
	 */
	public void connect() throws Exception, IllegalStateException;

	/**
	 * disconnects the client from the currently connected server.
	 */
	public void disconnect();

	/**
	 * Inserts a key-value pair into the KVServer.
	 *
	 * @param key
	 *            the key that identifies the given value.
	 * @param value
	 *            the value that is indexed by the given key.
	 * @return a message that confirms the insertion of the tuple or an error.
	 * @throws Exception
	 *             if put command cannot be executed (e.g. not connected to any
	 *             KV server).
	 */
	public KVMessage put(String key, String value) throws Exception,
			IllegalStateException;
}
