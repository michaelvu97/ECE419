package shared;

import app_kvClient.IKVClient;
import app_kvServer.IKVServer;

public final class ObjectFactory {
	/*
	 * Creates a KVClient object for auto-testing purposes
	 */
    public static IKVClient createKVClientObject() {
        return new app_kvClient.KVClient();
    }
    
    /*
     * Creates a KVServer object for auto-testing purposes
     */
	public static IKVServer createKVServerObject(int port, int cacheSize, String strategy, String ECSLoc, int ECSPort) {
		// return new app_kvServer.KVServer("some_name", port, cacheSize, strategy,"KV_SERVER_STORAGE",ECSLoc,ECSPort);
        return null;
	}
}