package app_kvServer;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.BindException;
import java.net.InetAddress;

import java.io.IOException;

import java.util.Set;
import java.util.HashSet;


import logger.LogSetup;

import cache.*;
import server.*;
import storage.*;
import shared.metadata.*;
import shared.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.apache.zookeeper.ZooKeeper;

public class KVServer implements IKVServer {
	
	private String _name;
	private int _port;
	private int _cacheSize;
	private boolean running = false;
	private ServerSocket serverSocket;
	private IKVServer.CacheStrategy _strategy;
	private String _hostName = null;

    private ClientAcceptor _clientAcceptor;    

	private IServerStore serverStore;
	private IMetaDataManager metaDataManager = new MetaDataManager(null, this);
	private HashRange serverHR = null;

	private Set<ClientConnection> clientConnections = new HashSet<ClientConnection>();

	private static Logger logger = Logger.getRootLogger();

	private IZKClient _zkClient = null;

	private IECSCommandReceiver _ecsConnection;

	private static String USAGE = "server <name> <port> <cache strategy> <cache size>\n" +
		"<name> is the server znode name\n" +
		"<port> is a valid unused local port, or 0 for new unused port\n" +
		"<cache strategy> is one of {LRU, FIFO, LFU}\n" + 
		"<cache size> is the size of the cache.";

	/**
	 * Start KV Server at given port
	 * @param port given port for storage server to operate
	 * @param cacheSize specifies how many key-value pairs the server is allowed
	 *           to keep in-memory
	 * @param strategy specifies the cache replacement strategy in case the cache
	 *           is full and there is a GET- or PUT-request on a key that is
	 *           currently not contained in the cache. Options are "FIFO", "LRU",
	 *           and "LFU".
	 */ 
	public KVServer(String znodeName, int port, int cacheSize, String strategy,
				String diskStorageStr, String ecsLoc, int ecsPort) {
		if (znodeName == null || znodeName.length() == 0)
			throw new IllegalArgumentException("znodeName invalid");
		if (port < 0)
			throw new IllegalArgumentException("port is negative");
		if (cacheSize < 0)
			throw new IllegalArgumentException("cache size cannot be negative");
		if(ecsPort < 0)
			throw new IllegalArgumentException("ecs port is negative");

		this._name = znodeName;
		this._port = port;
		this._cacheSize = cacheSize;

		strategy = strategy.toUpperCase();

		try {
			_ecsConnection = new ECSCommandReceiver(this, metaDataManager, 
					ecsLoc, ecsPort);
		} catch (Exception e) {
			logger.error("Could not connect to ECS!", e);
			_ecsConnection = null;
		}
		
		switch (strategy) {
			case "FIFO":
				_strategy = IKVServer.CacheStrategy.FIFO;
				break;
			case "LRU":
				_strategy = IKVServer.CacheStrategy.LRU;
				break;
			case "LFU":
				_strategy = IKVServer.CacheStrategy.LFU;
				break;
			default:
				throw new IllegalArgumentException("Invalid cache strategy: \"" + strategy + "\"");
		}

		// Create the cache
		ICache cache = new Cache(this._cacheSize, this._strategy);
		IDiskStorage diskStorage = new DiskStorage(diskStorageStr);

		this.serverStore = new ServerStoreSmart(cache, diskStorage);
		try {
			this._zkClient = new ZKClient("localhost:2181", this._name);
			// Test for now
			this._zkClient.registerNode();
		} catch (Exception e) {
			logger.error("Could not connect to zookeeper!", e);
			this._zkClient = null;
		}
	}
    public HashRange getServerHR(){
    	return serverHR;
    }	    
    public KVServer(String znodeName, int port, int cacheSize, String cacheStrategy, String ecsLoc, int ecsPort) {
        this(znodeName, port, cacheSize, cacheStrategy, "DEFAULT_STORAGE", ecsLoc, ecsPort);
    }
	
	@Override
	public int getPort(){
		return this._port;
	}

	@Override
    public String getHostname(){
		return this._hostName;
	}

	@Override
    public CacheStrategy getCacheStrategy(){
		return this._strategy;
	}

	@Override
    public int getCacheSize(){
		return this._cacheSize;
	}

	@Override
    public boolean inStorage(String key){
		return this.serverStore.inStorage(key);
	}

	@Override
    public boolean inCache(String key){
		return this.serverStore.inCache(key);
	}

	@Override
    public String getKV(String key) throws Exception {
		Utils.validateKey(key);
		return this.serverStore.get(key);
		
	}

	@Override
    public void putKV(String key, String value) throws Exception{
		Utils.validateKey(key);
		Utils.validateValue(value);
		this.serverStore.put(key, value);
	}

	@Override
    public void clearCache(){
		this.serverStore.clearCache();
	}

	@Override
    public Pair popInRange(HashRange hr){
		return this.serverStore.popInRange(hr);
	}

	@Override
    public void clearStorage(){
		this.serverStore.clearStorage();
	}

	@Override
    public void run(){
    	this.running = initializeServer();
    	if (this.running) {
            new Thread(this._ecsConnection).start();
    		this._clientAcceptor = new ClientAcceptor(this.serverSocket, this.serverStore, this.metaDataManager);
    		new Thread(this._clientAcceptor).start();
    	}
	}

	private boolean isRunning() {
        return this.running;
    }

	private boolean initializeServer() {
		logger.info("Initializing server...");
		
		try {
			serverSocket = new ServerSocket(_port);
			
			_port = serverSocket.getLocalPort();
			_hostName = serverSocket.getInetAddress().getHostName();

			logger.info("Server " + _hostName + " listening on port: " + serverSocket.getLocalPort());    
			logger.info("ServerCache: [" + _strategy + ":" + _cacheSize + "]");
            return true;
		}
		catch (IOException e) {
        	logger.error("Error! Cannot open server socket:");
            if(e instanceof BindException){
            	logger.error("Port " + _port + " is already bound!");
            }
            return false;
        }
	}

    // Abruptly stop the server without any additional actions
    // NOTE: this includes performing saving to storage.
	@Override
    public void kill() {
   		try {
			serverSocket.close();
			logger.info("Server socket is closed.");
		} catch (IOException e) {
			logger.error("Error! " + "Unable to close socket on port: " + _port, e);
		}
	}

    // Gracefully stop the server, can perform any additional actions.
	@Override
    public void close() {
		// TODO last.

    	// Do not accept new connections
		this.running = false;

		logger.info("Stopping client acceptor");
		// Stop the client acceptor
		// Client acceptor is responsible for cleaning up client threads
		this._clientAcceptor.stop();

		try {
			serverSocket.close();
			logger.info("Server socket is closed.");
		} catch (IOException e) {
			logger.error("Unable to close server socket on port: " + _port, e);
		}

		// TODO: clear cache?
	}

	@Override
	public MetaData getMetaData() {
		throw new IllegalStateException("NOT IMPLEMENTED"); // TODO
	}

	@Override
	public boolean isWriterLocked() {
		throw new IllegalStateException(); // TODO
	}

	@Override
	public void requestLock() {
		logger.warn("REQUESTLOCK NOT IMPLEMENTED");
	}

	@Override
	public void requestUnlock() {
		logger.warn("REQUESTUNLOCK NOT IMPLEMENTED");
	}

	@Override
	public IKVServer.ServerStateType getServerState() {
		throw new IllegalStateException(); // TODO
	}

	/**
    * Removes any entries from storage/cache that don't belong to the hash range.
    */
    @Override
    public void refocus(HashRange hr){
    	this.serverStore.flushStorage(hr);
    	//TODO only flush within a hash range (not sure its required so not doing until I am sure there is time)
    	clearCache();
	}

	public static void main(String[] args) {
		try {
			new LogSetup("logs/server.log", Level.ALL);
			if(args.length != 4) {
				System.out.println("Error! Invalid number of arguments!");
				System.out.println(USAGE);
			} else {
				String znodeName = args[0];
				int port = Integer.parseInt(args[1]);
				String cacheStrategy = args[2];
				int cacheSize = Integer.parseInt(args[3]);
				String ecsLoc = args[4];
				int ecsPort = Integer.parseInt(args[5]);
				IKVServer kvServer = null;
				try {
					kvServer = new KVServer(znodeName, port, cacheSize, cacheStrategy, "DISK_STORAGE_" + znodeName, ecsLoc, ecsPort);
				} catch (IllegalArgumentException iae) {
					System.out.println(iae.getMessage());
				}
				kvServer.run();

				// TODO: we might need a way to kill the server remotely
				while(true);
			}

		} catch (IOException ioe)
		{
			System.out.println("Could not start server, " + ioe);
		} finally {
			System.out.println("Server exited");
		}
	}
}
