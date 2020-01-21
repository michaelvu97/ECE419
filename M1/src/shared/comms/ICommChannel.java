package shared.comms;

import java.net.Socket;
import java.io.IOException;

public interface ICommChannel {
    /**
     * Gets the comm channel socket.
     */
    public Socket getSocket();
    
    /**
     * Sends a byte array message to the target.
     * Will add the appropriate header bytes to be able to be received.
     */
    public void sendBytes(byte[] bytes) throws IOException;

    /**
     * Blocks and reads the next message from the input stream. Comm header
     * bytes will be removed.
     * @return the original message (the bytes message from sendBytes). Returns
     * null if the stream has ended.
     */
    public byte[] recvBytes() throws IOException;
}