package app_kvServer;

import shared.comms.*;	
import shared.messages.*;
import shared.Deserializer;
import shared.Serializer;
import server.*;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.log4j.*;

/**
 * Represents a connection end point for a particular client that is 
 * connected to the server. This class is responsible for message reception 
 * and sending.
 */
public class ClientConnection implements Runnable {

	private static Logger logger = Logger.getRootLogger();
	
	private boolean isOpen;
	private static final int BUFFER_SIZE = 1024;
	private static final int DROP_SIZE = 128 * BUFFER_SIZE;
	
	private ICommChannel commChannel;
	private IServerStore serverStore;

	private Socket clientSocket;
	private InputStream input;
	private OutputStream output;
	
	/**
	 * Constructs a new CientConnection object for a given TCP socket.
	 * @param clientSocket the Socket object for the client connection.
	 */
	public ClientConnection(Socket clientSocket, IServerStore serverStore) {
		if (clientSocket == null)
			throw new NullPointerException("client socket is null");
		if (serverStore == null)
			throw new IllegalArgumentException("server store is null");

		this.clientSocket = clientSocket;
		this.serverStore = serverStore;

		this.isOpen = true;
		try {
			this.commChannel = new CommChannel(clientSocket);
		} catch (IOException ioe) {
			this.isOpen = false;
			logger.error("Failed to establish client comm channel", ioe);
		}
	}

	private KVServerResponseMessage handleRequest(KVClientRequestMessage request) {
		if (request == null)
			throw new IllegalArgumentException("request is null");

		KVClientRequestMessage.RequestType requestType = request.getType();

		if (requestType == KVClientRequestMessage.RequestType.PUT) {

			if (request.getValue().equals("null")) {
				return handleDelete((KVPutMessage) request);
			} else {
				return handlePut((KVPutMessage) request);
			}

		} else if (requestType == KVClientRequestMessage.RequestType.GET) {
			return handleGet((KVGetMessage) request);
		} else {
			throw new IllegalArgumentException("request is not valid");
		}
	}

	private KVServerResponseMessage handleGet(KVGetMessage getMessage) {
		String key = getMessage.getKey();

		String result = serverStore.get(key);
		if (result != null) {
			return new KVServerResponseMessage(KVMessage.StatusType.GET_SUCCESS, "SUCCESS<" + key + ","+ result + ">");
		} else {
			return new KVServerResponseMessage(KVMessage.StatusType.GET_ERROR, "GET_ERROR<" + key + ">");
		}
	}

	private KVServerResponseMessage handlePut(KVPutMessage putMessage) {
		String key = putMessage.getKey();
		String value = putMessage.getValue();

		IServerStore.PutResult putResult = serverStore.put(key, value);
		if (putResult == IServerStore.PutResult.INSERTED) {
			return new KVServerResponseMessage(KVMessage.StatusType.PUT_SUCCESS, "PUT_SUCCESS<" + key + "," + value + ">");
		} else if (putResult == IServerStore.PutResult.UPDATED) {
			return new KVServerResponseMessage(KVMessage.StatusType.PUT_UPDATE, "PUT_UPDATE<" + key + "," + value + ">");
		} else {
			return new KVServerResponseMessage(KVMessage.StatusType.PUT_ERROR, "PUT_ERROR<" + key + "," + value + ">");
		}
	}

	private KVServerResponseMessage handleDelete(KVPutMessage deleteMessage) {
		String key = deleteMessage.getKey();

		boolean success = serverStore.delete(key);
		if (success) {
			return new KVServerResponseMessage(KVMessage.StatusType.DELETE_SUCCESS, "DELETE_SUCCESS<" + key + ">");
		} else {
			return new KVServerResponseMessage(KVMessage.StatusType.DELETE_ERROR, "DELETE_ERROR<" + key + ">");
		}
	}
	
	/**
	 * Initializes and starts the client connection. 
	 * Loops until the connection is closed or aborted by the client.
	 */
	public void run() {
		logger.info("Client connection thread started");
		try {
			while(isOpen) { 

				try {

					byte[] requestBytes = commChannel.recvBytes();
					KVClientRequestMessage request = KVClientRequestMessage.Deserialize(requestBytes);
					
					KVServerResponseMessage response = handleRequest(request);
					logger.info("Sending response: " + response.getStatus() + ", " + response.getResponseMessage());
					commChannel.sendBytes(response.convertToBytes());

				} catch (IOException ioe){
					logger.error("Error! Connection could not be established!", ioe);
					isOpen = false;
				}
			}
		} finally {
			try {
				if (clientSocket != null) {
					input.close();
					output.close();
					clientSocket.close();
				}
			} catch (IOException ioe) {
				logger.error("Error! Unable to tear down connection!", ioe);
			}
		}
	}
}
