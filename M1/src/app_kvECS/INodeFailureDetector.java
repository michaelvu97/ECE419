package app_kvECS;

import java.util.List;

/**
 * Strictly informational, just gives informatino about what servers are up, and
 * informs ECS when a server goes down.
 */
public interface INodeFailureDetector extends Runnable {

    public interface IOnNodeFailedCallback {
        /**
         * A blocking callback for handling the node failed event. Will return
         * once the failure has been handled.
         */
        void onNodeFailed(String nodeName);
    }

    public void awaitStart();

    public void stop();

    /**
     * Returns the list of node names, that are currently alive.
     */
    public List<String> getAliveNodeNames();

    /**
     * Adds a listener to this detector. In the event that a node fails, the
     * callback object's method will be invoked.
     */
    public void addNodeFailedListener(IOnNodeFailedCallback callback);
}