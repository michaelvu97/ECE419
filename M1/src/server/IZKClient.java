package server;

public interface IZKClient {
    /**
     * Registers this server to zookeeper, using the given node name.
     */
    public void registerNode(String nodeName) throws Exception;
}