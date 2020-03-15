package app_kvECS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import logger.LogSetup;

import org.apache.zookeeper.*;
import org.apache.zookeeper.AsyncCallback;

public final class NodeFailureDetector implements INodeFailureDetector {

    private static Logger logger = Logger.getRootLogger();

    private List<IOnNodeFailedCallback> _nodeFailedCallbacks = 
            new ArrayList<IOnNodeFailedCallback>();

    private ZooKeeper _zooKeeper = null;
    private String _zkConnectString;
    private String _folderZNode;
    private CountDownLatch _connectionLatch = new CountDownLatch(1);

    private ClusterWatcher _clusterWatcher = null;

    public NodeFailureDetector(String zkConnectString, String folderZNode) {
        if (zkConnectString == null || zkConnectString.length() == 0)
            throw new IllegalArgumentException("zkConnectString is null");
        _zkConnectString = zkConnectString;
        _folderZNode = folderZNode;
    }

    @Override
    public void run() {
        logger.info("NodeFailureDetector running");
        try {
            _zooKeeper = new ZooKeeper(_zkConnectString, 5000, new Watcher() {
                public void process(WatchedEvent we) {
                    if (we.getState() == Watcher.Event.KeeperState.SyncConnected) {
                        _connectionLatch.countDown();
                        return;
                    }
                }
            });

            // Block until we are connected to zk
            _connectionLatch.await();
            _clusterWatcher = new ClusterWatcher(_zooKeeper, _folderZNode, this);
        } catch (IOException ioe){
            logger.error("Zookeeper connection failed", ioe);
            _zooKeeper = null; // Represents disconnected.
        } catch (InterruptedException ie) {
            logger.error("Zookeeper connection timedout", ie);
            _zooKeeper = null;
        }
    }

    private void validateConnected() {
        if (_zooKeeper == null) {
            throw new IllegalStateException("Zookeeper not connected");
        }
    }

    @Override
    public List<String> getAliveNodeNames() {
        validateConnected();
        try {
            return _zooKeeper.getChildren(_folderZNode, false);
        } catch (Exception e){
            logger.error("getAliveNodeNames failed", e);
            return null;
        }
    }

    @Override
    public void addNodeFailedListener(IOnNodeFailedCallback callback) {
        if (callback == null)
            throw new IllegalArgumentException("callback is null");

        _nodeFailedCallbacks.add(callback);
    }

    private void triggerNodeFailedListeners(String failedNodeName) {
        if (failedNodeName == null || failedNodeName.length() == 0)
            throw new IllegalArgumentException("failedNodeName is null/empty");

        for (IOnNodeFailedCallback cb : _nodeFailedCallbacks) {
            cb.onNodeFailed(failedNodeName);
        }
    }

    private class ClusterWatcher implements AsyncCallback.ChildrenCallback {

        private String _folderZNode;
        private ZooKeeper _zooKeeper;

        private Set<String> _currentChildren = new HashSet<String>();

        private NodeFailureDetector _parent;

        public ClusterWatcher(ZooKeeper zkClient, String folderZNode, 
                NodeFailureDetector parent) {
            if (zkClient == null)
                throw new IllegalArgumentException("zkClient is null");

            _folderZNode = folderZNode;

            _zooKeeper = zkClient;
            _parent = parent;

            _zooKeeper.getChildren(_folderZNode, true, this, null);
        }

        @Override
        public void processResult(int rc, String path, Object ctx, 
                List<String> children) {
            // ZK.GetChildren takes us here.
            
            List<String> deadChildren = new ArrayList<String>();

            for (String child : _currentChildren) {
                if (children.contains(child))
                    continue;

                deadChildren.add(child);
            }

            if (deadChildren.size() > 1) {
                logger.warn("More than one child died! (" + deadChildren.size() 
                        + ")");

                // This is sketch
                for (String deadChild : deadChildren) {
                    _parent.triggerNodeFailedListeners(deadChild);
                }
            } else if (deadChildren.size() == 1) {
                // A single child died
                logger.info("Child died, triggering callbacks");
                _parent.triggerNodeFailedListeners(deadChildren.get(0));
            }

            _currentChildren = new HashSet<String>(children);

            logger.debug("Current alive nodes: " + 
                    String.join(", ",   _currentChildren));

            // Watch for the next event.
            try {
                // Zookeeper API is actually the worst thing I've ever tried to
                // use. Please reconsider in the future, I would much rather
                // have written the whole heartbeat/failure detection
                // mechanisms myself. At least then, I would've learned 
                // something relevant to the course.
                Thread.sleep(2500 /* ms */);
            } catch (Exception e) {
                // Swallow
            }
            _zooKeeper.getChildren(_folderZNode, true, this, null);
        }
    }
}