package client;

import shared.messages.*;
import shared.Utils;

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

public class KVStore implements KVCommInterface {
	
	int port;
	String address; 

	private ICommChannel _commChannel = null;
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
			for(ClientSocketListener listener : listeners) {
				listener.handleStatus(SocketStatus.DISCONNECTED);
			}
		} catch (IOException ioe) {
			logger.error("Unable to close connection.");
		}
	}

	private void tearDownConnection() throws IOException {
		if (_clientSocket != null) {
			_clientSocket.close();
			_clientSocket = null;
			logger.info("Connection closed!");
		}
	}

	public void addListener(ClientSocketListener listener){
		listeners.add(listener);
	}

	@Override
	public KVMessage put(String key, String value) {
		try {
			Utils.validateKey(key);
			Utils.validateValue(value);

			KVClientRequestMessage message = KVClientRequestMessage.PUT(key, value);
			KVServerResponseMessage response = sendRequest(message);

			return response;
		} 
		catch (Exception e){
			// TODO
			return null;
		}
	}

	@Override
	public KVMessage get(String key){
		try {
			Utils.validateKey(key);

			KVClientRequestMessage message = KVClientRequestMessage.GET(key);
			KVServerResponseMessage response = sendRequest(message);

			return response;
		}
		catch (Exception e){
			// TODO
			return null;
		}
	}

	private KVServerResponseMessage sendRequest(KVClientRequestMessage requestMessage) throws IOException {
		// Inside here will be the actual marshalling of the message, and 
		// sending to the server.

		byte[] messageBytes = requestMessage.serialize();

		_commChannel.sendBytes(messageBytes);

		// Should probably trycatch?
		byte[] response = _commChannel.recvBytes();

		KVServerResponseMessage responseObj = KVServerResponseMessage.Deserialize(response);

		logger.info("Received server response: " + responseObj.toString());

		return responseObj;
	}
}
