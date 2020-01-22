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

	// Null when the socket is closed.
	private Socket clientSocket;

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
			this.clientSocket = null;
			logger.error("Failed to establish client comm channel", ioe);
		}
	}

	private KVMessage handleRequest(KVMessage request) {
		if (request == null)
			throw new IllegalArgumentException("request is null");

		KVMessage.StatusType requestType = request.getStatus();

		if (requestType == KVMessage.StatusType.PUT) {

			if (request.getValue().equals("null")) {
				return handleDelete(request);
			} else {
				return handlePut(request);
			}

		} else if (requestType == KVMessage.StatusType.GET) {
			return handleGet(request);
		} else {
			throw new IllegalArgumentException("request is not valid: " + request);
		}
	}

	private KVMessage handleGet(KVMessage getMessage) {
		String key = getMessage.getKey();

		String result = serverStore.get(key);
		if (result != null) {
			return new KVMessageImpl(KVMessage.StatusType.GET_SUCCESS, key, result);
		} else {
			return new KVMessageImpl(KVMessage.StatusType.GET_ERROR, key, null);
		}
	}

	private KVMessage handlePut(KVMessage putMessage) {
		String key = putMessage.getKey();
		String value = putMessage.getValue();

		IServerStore.PutResult putResult = serverStore.put(key, value);
		if (putResult == IServerStore.PutResult.INSERTED) {
			return new KVMessageImpl(KVMessage.StatusType.PUT_SUCCESS, key, value);
		} else if (putResult == IServerStore.PutResult.UPDATED) {
			return new KVMessageImpl(KVMessage.StatusType.PUT_UPDATE, key, value);
		} else {
			return new KVMessageImpl(KVMessage.StatusType.PUT_ERROR, key, value);
		}
	}

	private KVMessage handleDelete(KVMessage deleteMessage) {
		String key = deleteMessage.getKey();

		boolean success = serverStore.delete(key);
		if (success) {
			return new KVMessageImpl(KVMessage.StatusType.DELETE_SUCCESS, key, null);
		} else {
			return new KVMessageImpl(KVMessage.StatusType.DELETE_ERROR, key, null);
		}
	}

	/**
	 * Completes any pending request, disconnects the client, and then returns.
	 * Will block the caller until finished.
	 */
	public void stop() {
		logger.info("Stopping client connection thread.");
		this.isOpen = false;

		int millisSlept = 0;
		int sleepTimeoutMillis = 5000;

		while (clientSocket != null) {
			try {
				Thread.sleep(100);
			} catch (Exception e) {
				logger.warn("Thread stop sleep error", e);
				break;
			}
			millisSlept += 100;
			if (millisSlept < sleepTimeoutMillis) {
				logger.error("Thread stop timed out");
				break;
			}
		}

		// At this point, the socket is either closed or closing it has timed
		// out.
		return;
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
					
					KVMessage request = KVMessageImpl.Deserialize(requestBytes);
					KVMessage response = handleRequest(request);

					logger.info("Sending response: " + response);
					commChannel.sendBytes(response.serialize());

				} catch (IOException ioe){
					logger.error("Unexpectely lost connection to client", ioe);
					isOpen = false;
				} catch (Deserializer.DeserializationException dse) {
					logger.error("Received invalid message from client", dse);
					isOpen = false;
				}
			}
		} finally {
			try {
				if (clientSocket != null) {
					clientSocket.close();
					logger.info("Client disconnected, socket closed");
				}
			} catch (IOException ioe) {
				logger.error("Error! Unable to tear down connection!", ioe);
			} finally {
				clientSocket = null;
			}
		}
	}
}
