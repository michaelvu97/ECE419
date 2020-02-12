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
	
	int port;
	String address; 

	private ICommChannel _commChannel = null; // TODO remove

	private IServerCommManager _serverCommManager = new ServerCommManager();
	private Socket _clientSocket = null;
 	private Logger logger = Logger.getRootLogger();
 	private Set<ClientSocketListener> listeners;

	/**
	 * Initialize KVStore with address and port of KVServer
	 * @param address the address of the KVServer
	 * @param port the port of the KVServer
	 */

	public KVStore(String address, int port) {
		this.port = port;
		this.address = address;
	}

	@Override
	public void connect() throws UnknownHostException, IOException {
		_clientSocket = new Socket(address, port);
		listeners = new HashSet<ClientSocketListener>();
		logger.info("Connection established");

		_commChannel = new CommChannel(_clientSocket);
	}

	@Override
	public synchronized void disconnect() {
		logger.info("trying to close connection ...");
		try {
			tearDownConnection();
			for (ClientSocketListener listener : listeners) {
				listener.handleStatus(SocketStatus.DISCONNECTED);
			}
		} catch (IOException ioe) {
			logger.error("Unable to close connection.");
		} finally {
			_clientSocket = null;
		}
	}

	private void tearDownConnection() throws IOException {
		if (_clientSocket != null) {
			_clientSocket.close();
			logger.info("Connection closed!");
		}
	}

	public void addListener(ClientSocketListener listener){
		listeners.add(listener);
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

	private void validateConnected() {
		if (_clientSocket == null) {
			logger.error("Attempted to connect to a disconnected kvStore");
			throw new IllegalStateException("KVStore disconnected");
		}
	}
}
