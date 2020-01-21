package app_kvServer;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.BindException;
import java.net.InetAddress;

import java.io.IOException;

import java.util.Set;
import java.util.HashSet;

import logger.LogSetup;

import server.*;

import shared.Utils;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class KVServer implements IKVServer {
	
	private int _port;
	private int _cacheSize;
	private boolean running;
	private ServerSocket serverSocket;
	private IKVServer.CacheStrategy _strategy;
	private String _hostName = null;

	private IServerStore serverStore;

	private Set<ClientConnection> clientConnections = new HashSet<ClientConnection>();

	private static Logger logger = Logger.getRootLogger();

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
	public KVServer(int port, int cacheSize, String strategy){
		this._port = port;
		this._cacheSize = cacheSize;

		// Change this once a real implementation exists.
		this.serverStore = new ServerStoreDumb();
		
		switch (strategy.toLowerCase()) {
			case "fifo":
				_strategy = IKVServer.CacheStrategy.FIFO;
				break;
			case "lru":
				_strategy = IKVServer.CacheStrategy.LRU;
				break;
			case "lfu":
				_strategy = IKVServer.CacheStrategy.LFU;
				break;
			default:
				System.out.println("Invalid cache strategy: \"" + strategy + "\"");
				break;
		}
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
    public boolean inCache(String key){
		// TODO Auto-generated method stub
		return false;
	}

	@Override
    public String getKV(String key) throws Exception{
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
    public void clearStorage(){
		this.serverStore.clearStorage();
	}

	@Override
    public void run(){

    	running = initializeServer();

  		if(serverSocket != null){
	        while(isRunning()){
	            try {
	                Socket client = serverSocket.accept();                
	                
	                ClientConnection connection = new ClientConnection(
	                	client,
	                	this.serverStore
                	);

                	clientConnections.add(connection);

	                new Thread(connection).start();
	                
	                logger.info("Connected to " 
	                		+ client.getInetAddress().getHostName() 
	                		+  " on port " + client.getPort());
	            } catch (IOException e) {
	            	logger.error("Error! " + "Unable to establish connection. \n", e);
	            }
	        }
        }
        logger.info("Server socket is null.");
	}

	private boolean isRunning() {
        return this.running;
    }

	private boolean initializeServer() {
		logger.info("Initialize server ...");
		
		try {
			serverSocket = new ServerSocket(_port);
			// _hostName = serverSocket.getInetAddress().getHostName();
			_hostName = InetAddress.getLocalHost().getHostAddress();
			logger.info("Server " + _hostName + " listening on port: " + serverSocket.getLocalPort());    
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

		// Stop all existing connections
		for (ClientConnection conn : clientConnections) {
			conn.stop();
		}

		// TODO Clear/flush cache?
	}

	public static void main(String[] args) {
		try {
			new LogSetup("logs/server.log", Level.ALL);
			if(args.length != 1) {
				System.out.println("Error! Invalid number of arguments!");
				System.out.println("Usage: Server <port>!");
			} else {
				int port = Integer.parseInt(args[0]);
				IKVServer kvServer = new KVServer(port, 1000, "FIFO");
				kvServer.run();
			}

		} catch (IOException ioe)
		{
			System.out.println("Could not start server, " + ioe);
		} finally {
			System.out.println("Server exited");
		}
	}
}
