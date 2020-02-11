package testing;

import java.io.IOException;

import org.apache.log4j.Level;

import app_kvServer.KVServer;
import junit.framework.Test;
import junit.framework.TestSuite;
import logger.LogSetup;


public class AllTests {

	static {
		try {
			new LogSetup("logs/testing/test.log", Level.ERROR);
			KVServer kvServer = new KVServer(50000, 10, "FIFO", "STORAGE_FOR_TEST");
			kvServer.run();
			kvServer.clearStorage();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static Test suite() {
		TestSuite clientSuite = new TestSuite("Basic Storage ServerTest-Suite");
		clientSuite.addTestSuite(SerializationTest.class); 
		clientSuite.addTestSuite(ConnectionTest.class);
		clientSuite.addTestSuite(InteractionTest.class); 
		clientSuite.addTestSuite(AdditionalTest.class); 
		clientSuite.addTestSuite(DiskStorageTest.class);
		clientSuite.addTestSuite(KVMessageTest.class);
		clientSuite.addTestSuite(ServerStoreSmartTest.class);
		clientSuite.addTestSuite(UtilsTest.class);
		clientSuite.addTestSuite(CacheTest.class);
		clientSuite.addTestSuite(MetaDataTest.class);
		return clientSuite;
	}
	
}
