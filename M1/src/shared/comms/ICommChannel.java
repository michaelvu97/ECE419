package shared.comms;

import java.net.Socket;
import java.io.IOException;

public interface ICommChannel {

    /**
     * Returns true while the channel is open. If an endpoint fails, the channel
     * (and socket) will close.
     */
    public boolean isOpen();

    /**
     * Closes the comm channel.
     * No-op if the channel is already closed.
     */
    public void close();
    
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