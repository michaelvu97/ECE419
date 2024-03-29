package app_kvServer;

import shared.comms.*;	
import shared.messages.*;
import shared.network.*;	
import shared.serialization.*;
import server.*;
import shared.metadata.*;
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
public class ClientConnection extends Connection {

	private static Logger logger = Logger.getRootLogger();
	
	private static final int BUFFER_SIZE = 1024;
	private static final int DROP_SIZE = 128 * BUFFER_SIZE;
	
	private IServerStore serverStore;
	private IKVServer kvServer;

	private IMetaDataManager metaDataManager;

	// Null when the socket is closed.
	private Socket clientSocket;

	/**
	 * Constructs a new CientConnection object for a given TCP socket.
	 * @param clientSocket the Socket object for the client connection.
	 */
	public ClientConnection(Socket clientSocket, IServerStore serverStore, 
				IMetaDataManager metaDataManager, ClientAcceptor acceptor,
				IKVServer kvServer) {
		super(clientSocket, acceptor);

		if (serverStore == null)
			throw new IllegalArgumentException("server store is null");
		if (metaDataManager == null)
			throw new IllegalArgumentException("metaDataManager is null");

		this.serverStore = serverStore;
		this.metaDataManager = metaDataManager;
		this.kvServer = kvServer;
	}


	/**
	 * Initializes and starts the client connection. 
	 * Loops until the connection is closed or aborted by the client.
	 */
	@Override
	public void work() throws Exception{
		try {

			byte[] requestBytes = commChannel.recvBytes();
			
			KVMessage request = KVMessageImpl.Deserialize(requestBytes);
			KVMessage response = handleRequest(request);

			logger.info("Sending response: " + response);
			commChannel.sendBytes(response.serialize());

		} catch (IOException ioe){
			logger.warn("Unexpectely lost connection to client", ioe);
			isOpen = false;
		} catch (Deserializer.DeserializationException dse) {
			logger.error("Received invalid message from client", dse);
			isOpen = false;
		}
	}


	private KVMessage handleRequest(KVMessage request) {
		if (request == null)
			throw new IllegalArgumentException("request is null");

		KVMessage.StatusType requestType = request.getStatus();

		HashValue requestHash = null;
		if (request.getKey() != null)
			requestHash = HashUtil.ComputeHashFromKey(request.getKey());


		if (requestType == KVMessage.StatusType.PUT 
				|| requestType == KVMessage.StatusType.GET) {

			if (kvServer.getServerState() == IKVServer.ServerStateType.STOPPED) {
				return new KVMessageImpl(
					KVMessage.StatusType.SERVER_STOPPED,
					null,
					this.metaDataManager.getMetaData().serialize()
				);
			}

			// Verify that this server is responsible.
			boolean validForPut = metaDataManager.isInRange(requestHash);
			boolean validForGet = validForPut || metaDataManager.isInReplicaRange(requestHash);
			
			if ((requestType == KVMessage.StatusType.PUT && !validForPut) ||
				(requestType == KVMessage.StatusType.GET && !validForGet)) {
				logger.debug("Not responsible for key " + request.getKey());
				logger.debug("Sending metadata: " 
						+ this.metaDataManager.getMetaData().toString());
				
				return new KVMessageImpl(
					KVMessage.StatusType.SERVER_NOT_RESPONSIBLE,
					null,
					this.metaDataManager.getMetaData().serialize()
				);
			}
		}

		if (requestType == KVMessage.StatusType.PUT_BACKUP || requestType == KVMessage.StatusType.PUT_BACKUP_DUMP){
			if(requestType == KVMessage.StatusType.PUT_BACKUP && !this.metaDataManager.isInReplicaRange(requestHash)){
				return new KVMessageImpl(
					KVMessage.StatusType.INCORRECT_BACKUP_LOCATION,
					null,
					(byte[]) null
				);
			} else {
				return handlePutUpdate(request);
			}
		}

		if (requestType == KVMessage.StatusType.PUT || requestType == KVMessage.StatusType.PUT_DUMP) {
			if (kvServer.isWriterLocked()) {
				return new KVMessageImpl(
					KVMessage.StatusType.SERVER_WRITE_LOCK,
					null,
					(byte[]) null
				);
			}

			if (requestType == KVMessage.StatusType.PUT_DUMP)
				logger.debug("Received PUT_DUMP: " + request.getKey());

			if (request.getValue() == null || 
				request.getValue().equals("null") || 
				request.getValue().isEmpty()) {
				return handleDelete(request);
			} else {
				return handlePut(request);
			}

		} else if (requestType == KVMessage.StatusType.GET) {
			return handleGet(request);
		} else if (requestType == KVMessage.StatusType.GET_METADATA) {
			return handleGetMetadata(request);
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
			return new KVMessageImpl(KVMessage.StatusType.GET_ERROR, key);
		}
	}

	private KVMessage handlePutUpdate(KVMessage putMessage) {
		String key = putMessage.getKey();
		String value = putMessage.getValue();
		if (putMessage.getValue() == null || 
				putMessage.getValue().equals("null") || 
				putMessage.getValue().isEmpty()) {
			boolean success = serverStore.delete(key);
			if (success) {
				return new KVMessageImpl(KVMessage.StatusType.DELETE_SUCCESS, key);
			} else {
				return new KVMessageImpl(KVMessage.StatusType.DELETE_ERROR, key);
			}
		} else {
			IServerStore.PutResult putResult = serverStore.put(key, value);
			if (putResult == IServerStore.PutResult.INSERTED) {
				return new KVMessageImpl(KVMessage.StatusType.PUT_SUCCESS, key, value);
			} else if (putResult == IServerStore.PutResult.UPDATED) {
				return new KVMessageImpl(KVMessage.StatusType.PUT_UPDATE, key, value);
			} else {
				return new KVMessageImpl(KVMessage.StatusType.PUT_ERROR, key, value);
			}
		}
		
		
	}

	private KVMessage handlePut(KVMessage putMessage) {
		String key = putMessage.getKey();
		String value = putMessage.getValue();
		IServerStore.PutResult putResult = serverStore.put(key, value);
		kvServer.broadcastUpdateToReplicas(key, value);
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
		kvServer.broadcastUpdateToReplicas(key,"null");
		if (success) {
			return new KVMessageImpl(KVMessage.StatusType.DELETE_SUCCESS, key);
		} else {
			return new KVMessageImpl(KVMessage.StatusType.DELETE_ERROR, key);
		}
	}

	private KVMessage handleGetMetadata(KVMessage getMetadataMessage) {
		// No need to validate any fields, there are none.
		return new KVMessageImpl(
			KVMessage.StatusType.GET_METADATA_SUCCESS,
			null,
			this.metaDataManager.getMetaData().serialize()
		);
	}
}
