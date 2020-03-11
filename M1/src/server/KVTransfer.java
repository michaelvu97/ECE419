package server;

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
import client.*;

public class KVTransfer implements KVTransferInterface {
	private ICommChannel _commChannel = null;

 	private Logger logger = Logger.getRootLogger();

 	private ServerInfo _entryPointServerInfo;

	/**
	 * Initialize KVStore with an initial server to connect to.
	 */
	public KVTransfer(ServerInfo entryPointServerInfo) {
		if (entryPointServerInfo == null)
			throw new IllegalArgumentException("entryPointServerInfo is null");
		_entryPointServerInfo = entryPointServerInfo;
	}

	public KVTransfer(String name, String host, int port) {
		this(new ServerInfo(name, host, port));
	}


    @Override
    public void connect() throws IOException {
        logger.debug("Connecting to KVServer for transfer");

        Socket clientSocket = new Socket(
            _entryPointServerInfo.getHost(),
            _entryPointServerInfo.getPort()
        );
        _commChannel = new CommChannel(clientSocket);
        logger.info("Connection established");
    }

    @Override
    public synchronized void disconnect() {
    	if (_commChannel != null) {
  			_commChannel.close();
    	}
    	_commChannel = null;
    	logger.info("KVTransfer disconnected from KVServer");
    }

    public KVMessage sendRequest(KVMessage message) 
            throws Deserializer.DeserializationException, IOException {
        if (message.getKey() == null)
            throw new IllegalArgumentException("Message contains a null key");

        byte[] messageBytes = message.serialize();

        _commChannel.sendBytes(messageBytes);

        // Should probably trycatch?
        byte[] response = _commChannel.recvBytes();

        KVMessage responseObj = KVMessageImpl.Deserialize(response);

        logger.debug("Received server response: " + responseObj.toString());

        return responseObj;
    }

	@Override
	public KVMessage put(String key, String value) throws Exception {
		validateConnected();
		try {
			Utils.validateKey(key);
			Utils.validateValue(value);

			KVMessage message = new KVMessageImpl(KVMessage.StatusType.PUT_SERVER, key, value);
			KVMessage response = sendRequest(message);

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

	private void validateConnected() {
		if (_commChannel == null) {
			logger.error(
					"Attempted to connect to a disconnected KVServer Cloud");
			throw new IllegalStateException("KVServer Cloud Disconnected");
		}
	}
}
