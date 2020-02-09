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

    private String _configFilePath;

    private ZooKeeper _zoo = null;

    private static Logger logger = Logger.getRootLogger();

    public ECSClient(String configFilePath) {
        if (configFilePath == null || configFilePath.length() == 0)
            throw new IllegalArgumentException("configFilePath");

        _configFilePath = configFilePath;
    }

    @Override
    public boolean start() {
        
        try {
            _zoo = new ZooKeeper("localhost:2181", 10000, new ECSWatcher());
            // data monitor?
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
        // Start a single server.
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
        // Start Admin CLI

        ECSClient client = new ECSClient("aaaa");
        client.start();
    }
}
