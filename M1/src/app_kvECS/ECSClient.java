package app_kvECS;

import java.io.IOException;
import java.util.Map;
import java.util.Collection;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import logger.LogSetup;

import org.apache.zookeeper.*;

import ecs.IECSNode;

public class ECSClient implements IECSClient {

    private ZooKeeper _zoo = null;

    private static Logger logger = Logger.getRootLogger();

    @Override
    public boolean start() {
        
        try {
            _zoo = new ZooKeeper("localhost", 0, new ECSWatcher());
        } catch (IOException e){
            _zoo = null;
        }

        // TODO
        return false;
    }

    @Override
    public boolean stop() {
        // TODO
        return false;
    }

    @Override
    public boolean shutdown() {
        // TODO
        return false;
    }

    @Override
    public IECSNode addNode(String cacheStrategy, int cacheSize) {
        // TODO
        return null;
    }

    @Override
    public Collection<IECSNode> addNodes(int count, String cacheStrategy, int cacheSize) {
        // TODO
        return null;
    }

    @Override
    public Collection<IECSNode> setupNodes(int count, String cacheStrategy, int cacheSize) {
        // TODO
        return null;
    }

    @Override
    public boolean awaitNodes(int count, int timeout) throws Exception {
        // TODO
        return false;
    }

    @Override
    public boolean removeNodes(Collection<String> nodeNames) {
        // TODO
        return false;
    }

    @Override
    public Map<String, IECSNode> getNodes() {
        // TODO
        return null;
    }

    @Override
    public IECSNode getNodeByKey(String Key) {
        // TODO
        return null;
    }

    public static void main(String[] args) {
        // TODO

        ECSClient client = new ECSClient();
        client.start();
    }
}
