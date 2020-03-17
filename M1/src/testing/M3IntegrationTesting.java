package testing;

import java.io.File;

import org.junit.Test;
import junit.framework.TestCase;

import client.*;
import shared.metadata.*;
import shared.messages.*;
import app_kvECS.*;

public class M3IntegrationTesting extends TestCase {

	private static String DEFAULT_ECS_CONFIG = "./src/app_kvECS/ecs.config";

	private KVStore getKVS() {
		KVStore kvs = new KVStore(new ServerInfo("unknown?!", "localhost", 50000));
		try {
			kvs.connect();
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
		return kvs;
	}

	private void put(KVStore kvs, String key, String value) {
		try {
			KVMessage res = kvs.put(key, value);
			assertTrue(res.getStatus() == KVMessage.StatusType.PUT_SUCCESS ||
						res.getStatus() == KVMessage.StatusType.PUT_UPDATE);
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}

	private String get(KVStore kvs, String key) {
		try {
			KVMessage result = kvs.get(key);
			assertTrue(result.getStatus() == KVMessage.StatusType.GET_SUCCESS);
			return result.getValue();
		} catch (Exception e){
			e.printStackTrace();
			assertTrue(false);
		}
		return null;
	}

	private void runScript(String name) {
		String cmd[] = {
			name
		};

		try {
			Process proc = Runtime.getRuntime().exec(cmd);
			proc.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}

	@Override
	public void setUp() {
		runScript("./src/testing/integration_startup.sh");
	}

	@Override
	public void tearDown() {
		runScript("./src/testing/integration_teardown.sh");
	}

	@Test
	public void testBasic() {
		/**
		 * Create an ecs server, add 3 kv servers, perform simple I/O, make sure it works.
		 */
		IECSClient ecsClient = new ECSClient(DEFAULT_ECS_CONFIG, null);
		try {
			ecsClient.start();
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
		ecsClient.addNodes(3, "FIFO", 10);

		KVStore kvs = getKVS();
		put(kvs, "1", "1");
		put(kvs, "2", "2");
		put(kvs, "3", "3");
		put(kvs, "4", "4");

		assertTrue(get(kvs, "1").equals("1"));
		assertTrue(get(kvs, "2").equals("2"));
		assertTrue(get(kvs, "3").equals("3"));
		assertTrue(get(kvs, "4").equals("4"));

		ecsClient.shutdown();		
	}
}