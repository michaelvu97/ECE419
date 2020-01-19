package app_kvServer;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.BindException;

import java.io.IOException;

import org.apache.log4j.Logger;

public class KVServer implements IKVServer {
	
	private int _port;
	private int _cacheSize;
	private boolean running;
    private ServerSocket serverSocket;
	private IKVServer.CacheStrategy _strategy;

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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
    public CacheStrategy getCacheStrategy(){
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		return "";
	}

	@Override
    public void putKV(String key, String value) throws Exception{
		// TODO Auto-generated method stub
	}

	@Override
    public void clearCache(){
		// TODO Auto-generated method stub
	}

	@Override
    public void clearStorage(){
		// TODO Auto-generated method stub
	}

	@Override
    public void run(){

    	running = initializeServer();

  		if(serverSocket != null){
	        while(isRunning()){
	            try {
	                Socket client = serverSocket.accept();                
	                ClientConnection connection = new ClientConnection(client);
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
			logger.info("Server listening on port: " + serverSocket.getLocalPort());    
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

	@Override
    public void kill(){
		// TODO Auto-generated method stub
	}

	@Override
    public void close(){
		// TODO Auto-generated method stub
	}

	public static void main(String[] args) {
		try {
			if(args.length != 1) {
				System.out.println("Error! Invalid number of arguments!");
				System.out.println("Usage: Server <port>!");
			} else {
				int port = Integer.parseInt(args[0]);
				IKVServer kvServer = new KVServer(port, 1000, "FIFO");
				kvServer.run();
			}

		} finally {
			System.out.println("Server exited");
		}
	}
}
