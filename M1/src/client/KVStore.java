package client;

import shared.messages.*;
import shared.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.UnsupportedOperationException;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

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
		Utils.validateKey(key);
		Utils.validateValue(value);

		KVClientRequestMessage message = new KVPutMessage(key, value);
		KVServerResponseMessage response = send(message);

		return response;
	}


	@Override
	public KVMessage get(String key) throws Exception {
		Utils.validateKey(key);

		KVClientRequestMessage message = new KVGetMessage(key);
		KVServerResponseMessage response = send(message);

		return response;
	}

	private Logger logger = Logger.getRootLogger();
	private OutputStream output;
 	private InputStream input;	

	private KVServerResponseMessage send(KVClientRequestMessage requestMessage) throws Exception {
		// Inside here will be the actual marshalling of the message, and 
		// sending to the server.

		byte[] messageBytes = requestMessage.convertToBytes();

		// Should probably trycatch?
		byte[] response = sendBytes(messageBytes);
		
		// TODO deserialize response.
		return null; // For now.
	}
	
	//WARNING: NOT CONFIRMED WORKING
	private byte[] sendBytes (byte[] msgBytes) throws IOException {
		//Send Message
		//get size of array
		int msgSize = msgBytes.length;
		//add the size to the first 4 bytes of the array
		byte [] msgToSend = new byte [msgSize+4];
		msgToSend[0] = (byte) (msgSize>>24);
		msgToSend[1] = (byte) (msgSize>>16);
		msgToSend[2] = (byte) (msgSize>>8);
		msgToSend[3] = (byte) (msgSize);
		//copy old array into the remaining bytes
		System.arraycopy(msgBytes,0,msgToSend,4,msgSize);

		//check if it is within specified parameters
		if(msgSize < 1 || msgSize > 1048576){
			logger.error("'Input message too small/large, no message sent'");
			return null;
		}

		output.write(msgToSend, 0, msgToSend.length);
		output.flush();
		logger.info("'Message sent, awaiting response'");
		
		//get input_size as the first 4 bytes of the message
		int responseSize = ((int) input.read())<<24;
		responseSize += ((int) input.read())<<16;
		responseSize += ((int) input.read())<<8;
		responseSize += ((int) input.read());
		//check if the message is of size 0 or too big. if so, return NULL 
		if(responseSize <1){
			logger.error("'Message size incorrect from server'");
			return null;
		}

		//setup receivedMsg to be of size of the input size
		byte [] receivedMsg = new byte[responseSize];
		//read input size bytes
		for(int i = 0; i<responseSize || i<1048576;i++){
			//read char from input stream
			receivedMsg[i] = (byte) input.read();
		}
		
		/* return response string */
		logger.info("'Response received'");
		return receivedMsg;
    }
}
