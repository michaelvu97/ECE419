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

import shared.ZooKeeperConstants;

public class ZKClient implements IZKClient {
    
    private static Logger logger = Logger.getRootLogger();

    private ZooKeeper _zooKeeper = null;

    private String _nodeName;

    private CountDownLatch _connectionLatch = new CountDownLatch(1);

    private String getPath() {
        return ZooKeeperConstants.APP_FOLDER + "/" + _nodeName;
    }

    public ZKClient(String ecsHostName, String nodeName) throws Exception {
        if (nodeName == null || nodeName.length() == 0) 
            throw new IllegalArgumentException("Node name invalid");
        _nodeName = nodeName;

        _zooKeeper = new ZooKeeper(
                ecsHostName + ":" + ZooKeeperConstants.ZK_PORT,
                ZooKeeperConstants.TIMEOUT,
                new Watcher() {
                    public void process(WatchedEvent we) {
                        if (we.getState() == 
                                    Watcher.Event.KeeperState.SyncConnected) {
                            _connectionLatch.countDown();
                            return;
                        }
                    }
                }
        );

        _connectionLatch.await();
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

    public void close() {
        try {
            _zooKeeper.close();
        } catch (Exception e) {
            logger.warn("failed to kill zkclient", e);
        }
    }
}