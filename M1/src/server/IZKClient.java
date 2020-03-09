package server;

public interface IZKClient {
    /**
     * Registers this server to zookeeper.
     */
    public void registerNode() throws Exception;

    public void close();
}