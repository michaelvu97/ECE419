package shared.comms;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;

import org.apache.log4j.Logger;

public class CommChannel implements ICommChannel {
    private Socket _socket;
    private Logger _logger = Logger.getRootLogger();

    public CommChannel(Socket socket) {
        if (socket == null)
            throw new IllegalArgumentException("Socket may not be null");
        
        _socket = socket;
    }

    @Override
    public Socket getSocket() {
        return _socket;
    }

    @Override
    public void sendBytes(byte[] bytes) throws IOException {
        int msgSize = bytes.length;

        // Add the size to the first 4 bytes of the array
        byte [] msgToSend = new byte [msgSize + 4];
        msgToSend[0] = (byte) (msgSize>>24);
        msgToSend[1] = (byte) (msgSize>>16);
        msgToSend[2] = (byte) (msgSize>>8);
        msgToSend[3] = (byte) (msgSize);

        // Copy old array into the remaining bytes
        System.arraycopy(bytes, 0, msgToSend, 4, msgSize);

        // Check if it is within specified parameters
        if (msgSize < 1 || msgSize > 1048576){
            _logger.error("'Input message too small/large, no message sent'");
            return;
        }

        _output.write(msgToSend, 0, msgToSend.length);
        _output.flush();
        _logger.info("Sent " + msgSize + " bytes (" + msgToSend.length + " bytes total)");
    }

    @Override
    public byte[] recvBytes() throws IOException {
        // Get input_size as the first 4 bytes of the message
        int messageSize = ((int) _input.read()) << 24;
        messageSize |= ((int) _input.read()) << 16;
        messageSize |= ((int) _input.read()) << 8;
        messageSize |= ((int) _input.read());

        _logger.debug("Read header, size=" + messageSize);

        // Check if the message is of size 0 or too big. if so, return NULL 
        if (messageSize < 1){
            _logger.error("Message size invalid: " + messageSize);

            // TODO: should this be an exception instead?
            return null;
        }

        // Setup receivedMsg to be of size of the input size
        byte[] receivedMsg = new byte[messageSize];

        // Read input size bytes
        for(int i = 0; i < messageSize && i < 1048576; i++){
            //read char from input stream
            receivedMsg[i] = (byte) _input.read();
        }
        
        /* Return response string */
        _logger.info("Received " + messageSize + " byte message");
        return receivedMsg;
    }
}