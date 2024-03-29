package app_kvECS;

import java.util.*;
import java.util.Collection;

import ecs.*;
import shared.metadata.*;

public interface IECSClient extends INodeFailureDetector.IOnNodeFailedCallback {

    public List<ServerInfo> getAllServerInfo();
    
    public void setAllServers(String configFilePath);

    /**
     * Starts the storage service by calling start() on all KVServer instances that participate in the service.\
     * @throws Exception    some meaningfull exception on failure
     * @return  true on success, false on failure
     */
    public boolean start() throws Exception;

    /**
     * Stops the service; all participating KVServers are stopped for processing client requests but the processes remain running.
     * @throws Exception    some meaningfull exception on failure
     * @return  true on success, false on failure
     */
    public boolean stop() throws Exception;

    /**
     * Stops all server instances and exits the remote processes.
     * @return  true on success, false on failure
     */
    public boolean shutdown();

    /**
     * Create a new KVServer with the specified cache size and replacement strategy and add it to the storage service at an arbitrary position.
     * @return  name of new server
     */
    public ECSNode addNode(String cacheStrategy, int cacheSize);

    /**
     * Randomly choose <numberOfNodes> servers from the available machines and start the KVServer by issuing an SSH call to the respective machine.
     * This call launches the storage server with the specified cache size and replacement strategy. For simplicity, locate the KVServer.jar in the
     * same directory as the ECS. All storage servers are initialized with the metadata and any persisted data, and remain in state stopped.
     * NOTE: Must call setupNodes before the SSH calls to start the servers and must call awaitNodes before returning
     * @return  set of strings containing the names of the nodes
     */
    public List<ECSNode> addNodes(int count, String cacheStrategy, int cacheSize);

    /**
     * Sets up `count` servers with the ECS (in this case Zookeeper)
     * @return  array of strings, containing unique names of servers
     */
    public List<ECSNode> setupNodes(int count, String cacheStrategy, int cacheSize);

    /**
     * Wait for all nodes to report status or until timeout expires
     * @param count     number of nodes to wait for
     * @param timeout   the timeout in milliseconds
     * @return  true if all nodes reported successfully, false otherwise
     */
    public boolean awaitNodes(int count, int timeout) throws Exception;

    // public void setServerAvailable(String serverName);

    /**
     * Removes nodes with names matching the nodeNames array
     * @param nodeNames names of nodes to remove
     * @return  true on success, false otherwise
     */
    public List<String> removeNodes(List<String> nodeNames);
    public boolean removeNode(String nodeName);

    /**
     * Get a map of all nodes
     */
    public Map<String, ECSNode> getNodes();

    /**
     * Kills the nodes specifed by user by broadcasting the list of server names 
     * to be killed. Servers then invoke kill on themselves if they are in the list.
     */
    public List<String> killNodes(List<String> nodeNames);
    public boolean killNode(String nodeName);
    
    /**
     * Get the specific node responsible for the given key
     */
    public ECSNode getNodeByName(String name);

    /**
     * Get the next server that is available. 
     */
    public ServerInfo getNextAvailableServer();

    public void signalNodeConnected(String name);

}
