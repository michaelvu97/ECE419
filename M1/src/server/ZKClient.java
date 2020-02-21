package server;

import java.util.ArrayList;
import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import logger.LogSetup;

import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.KeeperException;

public class ZKClient implements IZKClient {
    
    private static Logger logger = Logger.getRootLogger();

    private ZooKeeper _zooKeeper = null;

    public ZKClient(String connectString) throws Exception {
        _zooKeeper = new ZooKeeper(connectString, 10000, null /* TODO*/);
        attemptRegisterApp();
    }

    private void attemptRegisterApp() {
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
    public void registerNode(String nodeName) throws Exception {
        String path = _zooKeeper.create(
            "/kvclients/" + nodeName,
            "some_data".getBytes(),
            ZooDefs.Ids.OPEN_ACL_UNSAFE,
            CreateMode.EPHEMERAL
        );
        logger.debug("Registered node: " + path);
    }

}