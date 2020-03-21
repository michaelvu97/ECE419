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
import java.util.*;
import shared.metadata.*;
import shared.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import java.net.UnknownHostException;
import org.apache.zookeeper.ZooKeeper;

public class KVServer implements IKVServer {
	
	private String _name;
	private int _port;
	private int _cacheSize;
	private boolean running = false;
	private ServerSocket serverSocket;
	private IKVServer.CacheStrategy _strategy;
	private String _hostName = null;
    private boolean _writeLocked = false;

    private ServerStateType _state = ServerStateType.STARTED;

    private ClientAcceptor _clientAcceptor;    

	private IServerStore serverStore;
	private IMetaDataManager metaDataManager = null;

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
	public KVServer(String znodeName, String host,  int port, int cacheSize, String strategy,
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
		this._hostName = host;

		metaDataManager = new MetaDataManager(null, this);

		// logger.info("ENTERING");
		// HashValue serverHash = HashUtil.ComputeHash(this._hostName,this._port);
		// logger.info("1");
		// MetaDataSet temp = metaDataManager.getMetaData();
		// logger.info("2");
		// this.replica1 = temp.getReplicaForHash(serverHash,0);
		// logger.info("3");
		// this.replica1 = temp.getReplicaForHash(serverHash,1);
		// logger.info("WE GOT PAST IT");
		// strategy = strategy.toUpperCase();

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
			this._zkClient = new ZKClient(ecsLoc, this._name);
			// Test for now
			this._zkClient.registerNode();
		} catch (Exception e) {
			logger.error("Could not connect to zookeeper!", e);
			this._zkClient = null;
		}
	}
	
    public KVServer(String znodeName, String host, int port, int cacheSize, String cacheStrategy, String ecsLoc, int ecsPort) {
        this(znodeName, host, port, cacheSize, cacheStrategy, "DEFAULT_STORAGE", ecsLoc, ecsPort);
    }

    @Override
    public String getName() {
    	return this._name;
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
    public List<Pair> getAllInRange(HashRange hr) {
		return this.serverStore.getAllInRange(hr);
	}

	// this function now returns true or false based on transfer success or failure.
	@Override    
    public boolean transferDataToServer(MetaData serverToSendTo) {
		ServerInfo transferserver = new ServerInfo(serverToSendTo.getName(), serverToSendTo.getHost(), serverToSendTo.getPort());
    	KVTransfer transferClient = new KVTransfer(transferserver);
    	
    	try {
    		transferClient.connect();
    	}
    	catch (UnknownHostException unknown) {
    		logger.error("Could not connect to given server! (unknown host)");
    		return false;
    	}
    	catch (IOException io) {
    		logger.error("Could not connect to given server! (i/o)");
    		return false;
    	}

    	List<Pair> toTransfer = getAllInRange(serverToSendTo.getHashRange());
        logger.info("Transferring " + toTransfer.size() + " items to " + serverToSendTo.getName());
    	for (Pair KV : toTransfer) {
    		try{
    			transferClient.put_dump(KV.k,KV.v);
    		}
    		catch (Exception ex) {
				logger.error("Could not tranfser KV pair <" + KV.k + "," + KV.v + ">");
				return false;
    		}
    	}
		transferClient.disconnect();
		return true;
    }

    // sends to replicas. Returns false if a failure occurs
	@Override    
    public boolean broadcastUpdateToReplicas(String key, String value) {
    	
		for (MetaData replica : metaDataManager.getReplicas()) {
			if (replica == null)
				continue;

			ServerInfo transferserver = new ServerInfo(
					replica.getName(),
					replica.getHost(),
					replica.getPort()
			);
	    	KVTransfer transferClient = new KVTransfer(transferserver);
	    	
	    	try {
	    		transferClient.connect();
	    	} catch (UnknownHostException unknown) {
	    		logger.error("Could not connect to given server!", unknown);
	    		return false;
	    	} catch (IOException io) {
	    		logger.error("Could not connect to given server!", io);
	    		return false;
	    	}

	        logger.info("Sending to replica: " + replica.getName());

			try{
				transferClient.put_backup(key, value);
			}
			catch (Exception ex) {
				logger.error("Could not tranfser KV pair <" + key + "," + 
						value + ">", ex);
				return false;
			}
			transferClient.disconnect();
		}
		return true;
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
    		this._clientAcceptor = new ClientAcceptor(this.serverSocket, this.serverStore, this.metaDataManager, this);
    		new Thread(this._clientAcceptor).start();
    	}
	}

	private boolean isRunning() {
        return this.running;
    }

	private boolean initializeServer() {
		logger.info("Initializing server on port " + _port);
		
		try {
			serverSocket = new ServerSocket(_port);
			
			_port = serverSocket.getLocalPort();
			_hostName = serverSocket.getInetAddress().getHostName();

			logger.info("Server " + _hostName + " listening on port: " + serverSocket.getLocalPort());    
			logger.info("ServerCache: [" + _strategy + ":" + _cacheSize + "]");
            return true;
		}
		catch (IOException e) {
        	logger.error("Error! Cannot open server socket", e);
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
        logger.warn("Server killed!");
   		System.exit(0);
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
        this._zkClient.close();

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
		return metaDataManager.getMyMetaData();
	}

	@Override
	public boolean isWriterLocked() {
		return _writeLocked;
	}

	@Override
	public void writeLock() {
		_writeLocked = true;
	}

	@Override
	public void writeUnlock() {
		_writeLocked = false;
	}

	@Override
	public ServerStateType getServerState() {
		return _state;
	}

    @Override
    public void setServerState(ServerStateType state) {
        _state = state;
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
			if(args.length != 7) {
				System.out.println("Error! Invalid number of arguments!");
				System.out.println(USAGE);
			} else {
				new LogSetup("logs/kvserver-" + args[0] + ".log", Level.ALL);
				String znodeName = args[0];
				String host = args[1];
				int port = Integer.parseInt(args[2]);
				String cacheStrategy = args[3];
				int cacheSize = Integer.parseInt(args[4]);
				String ecsLoc = args[5];
				int ecsPort = Integer.parseInt(args[6]);
				IKVServer kvServer = null;
				try {
					kvServer = new KVServer(znodeName, host, port, cacheSize, cacheStrategy, "DISK_STORAGE_" + znodeName, ecsLoc, ecsPort);
				} catch (IllegalArgumentException iae) {
					System.out.println(iae.getMessage());
				}
				kvServer.run();
				logger.info("KV SERVER RUNNING");

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
