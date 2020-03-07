package server;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import logger.LogSetup;

import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

public class ZKClient implements IZKClient {
    
    private static Logger logger = Logger.getRootLogger();

    private ZooKeeper _zooKeeper = null;

    private String _nodeName;

    private CountDownLatch _connectionLatch = new CountDownLatch(1);

    private String getPath() {
        return "/kvclients/" + _nodeName;
    }

    public ZKClient(String connectString, String nodeName) throws Exception {
        if (nodeName == null || nodeName.length() == 0) 
            throw new IllegalArgumentException("Node name invalid");
        _nodeName = nodeName;

        _zooKeeper = new ZooKeeper(connectString, 10000, new Watcher() {
            public void process(WatchedEvent we) {
                if (we.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    _connectionLatch.countDown();
                    return;
                }
                if (we.getPath().equals(getPath())) {
                    // Event involving this node.
                    if (we.getType() == Watcher.Event.EventType.NodeDataChanged) {

                    }
                }
            }
        });

        _connectionLatch.await();

        attemptRegisterApp();
    }

    private void attemptRegisterApp() {
        // TODO, move this to ECS instead.
        try {
            // TODO: some of this might be wrong.
            String path = _zooKeeper.create(
                "/kvclients",
                "this_is_the_app_folder".getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT
            );
            logger.info("Created zk path: " + path);
        } catch (Exception e) {
            logger.warn("Could not register app", e);
        }
    }

    @Override
    public void registerNode() throws Exception {
        try {
            _zooKeeper.delete(getPath(), -1);
        } catch (Exception e) {
            // Nothing
        }

        String path = _zooKeeper.create(
            getPath(),
            "some_data".getBytes(),
            ZooDefs.Ids.OPEN_ACL_UNSAFE,
            CreateMode.EPHEMERAL
        );
        logger.debug("Registered node: " + path);
    }

}