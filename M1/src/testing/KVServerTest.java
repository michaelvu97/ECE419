package testing;

import org.junit.Test;

import app_kvClient.*;
import app_kvServer.*;
import junit.framework.TestCase;
import java.io.IOException;
import shared.messages.KVMessage;
import shared.messages.KVMessage.StatusType;
import org.apache.log4j.Logger;

public class KVServerTest extends TestCase {

	private KVServer kvServer = null;
	private KVClient kvClient = null;
	private Logger logger = Logger.getRootLogger();

	@Override
	public void setUp() {
		try {
			kvServer = new KVServer(0, 100, "FIFO", "STORAGE_FOR_TEST_SERVER");
			kvServer.run();

			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				logger.warn(e);
			}
			
			while (kvClient == null) {
				kvClient = new KVClient();
				kvClient.newConnection(kvServer.getHostname(), kvServer.getPort());
			}

		} catch (IOException ioe){
			logger.error(ioe);
		} catch (IllegalArgumentException iae) {
			System.out.println(iae.getMessage());
		} catch (Exception e) {
			logger.error(e);
		}
	}
	
	@Override
	public void tearDown() {
		kvClient.getStore().disconnect();
		kvServer.kill();
	}

	@Test
	public void testPut() {
		Exception ex = null;
		try {
			kvServer.putKV("k1", "v1");
	        //kvClient.getStore().put("k1", "v1");
			//assertTrue(kvServer.getKV("k1").equals("v1"));
		} catch (Exception e){ 
			ex = e;
			logger.error(e);
		}
		assertTrue(ex == null);

	}
}