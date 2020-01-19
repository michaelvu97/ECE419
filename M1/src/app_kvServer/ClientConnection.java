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

					KVClientRequestMessage.RequestType requestType = request.getType();
					if (requestType == KVClientRequestMessage.RequestType.PUT) {
							if (request.getValue().equals("null")) {
								logger.info("DELETE: " + request.getKey());
								serverStore.delete(request.getKey());
							} else {
								logger.info("PUT: " + request.getKey() + ", " + request.getValue());
								serverStore.put(request.getKey(), request.getValue());
							}
					} else if (requestType == KVClientRequestMessage.RequestType.GET) {
						logger.info("GET: " + request.getKey());
						String result = serverStore.get(request.getKey());
					} else {
						logger.error("Received invalid request from client");
					}

					// This is a dummy response for now
					KVServerResponseMessage response = new KVServerResponseMessage(KVMessage.StatusType.GET_SUCCESS, "boi you got it");

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
