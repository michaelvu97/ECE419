package client;

import shared.messages.*;
import shared.Utils;
import shared.serialization.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.UnsupportedOperationException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.HashSet;

import org.apache.log4j.Logger;

import client.ClientSocketListener.SocketStatus;

import shared.comms.*;
import shared.metadata.*;

public class KVStore implements KVCommInterface {
	protected IServerCommManager _serverCommManager;
 	protected Logger logger = Logger.getRootLogger();

	/**
	 * Initialize KVStore with an initial server to connect to.
	 */
	public KVStore(ServerInfo entryPointServerInfo) {
		if (entryPointServerInfo == null)
			throw new IllegalArgumentException("entryPointServerInfo is null");
		_serverCommManager = new ServerCommManager(entryPointServerInfo);
	}

	@Override
	public void connect() throws UnknownHostException, IOException {
		try {
			_serverCommManager.connect();
		} catch (UnknownHostException uhe) {
			logger.error("Host not found!");
			throw uhe;
		} finally {

		}
		logger.info("Connected to KVServer cloud");
	}

	@Override
	public synchronized void disconnect() {
		logger.info("trying to close connection ...");
		try {
			if (_serverCommManager != null)
				_serverCommManager.disconnect();
			logger.info("Disconnected from KVServer cloud");
		} catch (IOException ioe) {
			logger.error("Unable to close connection to KVServer cloud");
		} finally {
			_serverCommManager = null;
		}
	}

	@Override
	public KVMessage put(String key, String value) throws Exception {
		validateConnected();
		try {
			Utils.validateKey(key);
			Utils.validateValue(value);

			KVMessage message = new KVMessageImpl(KVMessage.StatusType.PUT, key, value);
			KVMessage response = _serverCommManager.sendRequest(message);

			return response;
		} 
		catch (IOException ioe){
			logger.error("PUT failed I/O", ioe);
			throw ioe;
		}
		catch (Deserializer.DeserializationException dse) {
			logger.error("PUT failed, invalid server response", dse);
			throw dse;
		}
	}

	@Override
	public KVMessage get(String key) throws Exception {
		validateConnected();
		try {
			Utils.validateKey(key);

			KVMessage message = new KVMessageImpl(
				KVMessage.StatusType.GET,
				key
			);

			KVMessage response = _serverCommManager.sendRequest(message);

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

	protected void validateConnected() {
		if (_serverCommManager == null) {
			logger.error(
					"Attempted to connect to a disconnected KVServer Cloud");
			throw new IllegalStateException("KVServer Cloud Disconnected");
		}
	}
}
