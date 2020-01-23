package testing;

import org.junit.Test;

import junit.framework.TestCase;
import app_kvServer.*;
import cache.*;
import server.IServerStore;

public class CacheTest extends TestCase {
	
	private ICache cache; 
	private IKVServer.CacheStrategy _strategy;
	private int _cacheSize;

	@Test
	public void testPutGet() {
		_strategy = IKVServer.CacheStrategy.FIFO;
		_cacheSize = 6;
		cache = new Cache(_cacheSize,_strategy);

		String key1 = "Key1";
		String value1 = "Value1";
		String key2 = "Key2";
		String value2 = "Value2";
		String key3 = "Key3";
		String value3 = "Value3";
		String key4 = "Key4";
		String value4 = "Value4";
		String key5 = "Key5";
		String value5 = "Value5";
		String key6 = "Key6";
		String value6 = "Value6";

		cache.put(key1,value1);
		cache.put(key2,value2);
		cache.put(key3,value3);
		cache.put(key4,value4);
		cache.put(key5,value5);
		cache.put(key6,value6);

		assertTrue(value1 == cache.get(key1));
		assertTrue(value2 == cache.get(key2));
		assertTrue(value3 == cache.get(key3));
		assertTrue(value4 == cache.get(key4));
		assertTrue(value5 == cache.get(key5));
		assertTrue(value6 == cache.get(key6));
	}

	@Test
	public void testDelete() {
		_strategy = IKVServer.CacheStrategy.FIFO;
		_cacheSize = 6;
		cache = new Cache(_cacheSize,_strategy);

		Exception ex = null;
		String key1 = "Key1";
		String value1 = "Value1";
		String key2 = "Key2";
		String value2 = "Value2";
		String key3 = "Key3";
		String value3 = "Value3";

		cache.put(key1,value1);
		cache.put(key2,value2);
		cache.put(key3,value3);

		assertTrue(value1 == cache.get(key1));
		assertTrue(value2 == cache.get(key2));
		assertTrue(value3 == cache.get(key3));

		cache.delete(key2);

		assertTrue(value1 == cache.get(key1));
		assertTrue(null == cache.get(key2));
		assertTrue(value3 == cache.get(key3));
	}

	@Test
	public void testClear() {
		_strategy = IKVServer.CacheStrategy.FIFO;
		_cacheSize = 6;
		cache = new Cache(_cacheSize,_strategy);

		Exception ex = null;
		String key1 = "Key1";
		String value1 = "Value1";
		String key2 = "Key2";
		String value2 = "Value2";
		String key3 = "Key3";
		String value3 = "Value3";

		cache.put(key1,value1);
		cache.put(key2,value2);
		cache.put(key3,value3);

		cache.clear();

		assertTrue(null == cache.get(key1));
		assertTrue(null == cache.get(key2));
		assertTrue(null == cache.get(key3));
	}

	@Test
	public void testFIFO() {
		_strategy = IKVServer.CacheStrategy.FIFO;
		_cacheSize = 3;
		cache = new Cache(_cacheSize,_strategy);

		Exception ex = null;
		String key1 = "Key1";
		String value1 = "Value1";
		String key2 = "Key2";
		String value2 = "Value2";
		String key3 = "Key3";
		String value3 = "Value3";
		String key4 = "Key4";
		String value4 = "Value4";
		String key5 = "Key5";
		String value5 = "Value5";
		String key6 = "Key6";
		String value6 = "Value6";

		cache.put(key1,value1);
		cache.put(key2,value2);
		cache.put(key3,value3);
		assertTrue(value1 == cache.get(key1));
		assertTrue(value2 == cache.get(key2));
		assertTrue(value3 == cache.get(key3));

		cache.put(key4,value4);
		assertTrue(null == cache.get(key1));
		assertTrue(value2 == cache.get(key2));
		assertTrue(value3 == cache.get(key3));
		assertTrue(value4 == cache.get(key4));

		cache.put(key5,value5);
		assertTrue(null == cache.get(key1));
		assertTrue(null == cache.get(key2));
		assertTrue(value3 == cache.get(key3));
		assertTrue(value4 == cache.get(key4));
		assertTrue(value5 == cache.get(key5));

		cache.put(key6,value6);

		assertTrue(null == cache.get(key1));
		assertTrue(null == cache.get(key2));
		assertTrue(null == cache.get(key3));
		assertTrue(value4 == cache.get(key4));
		assertTrue(value5 == cache.get(key5));
		assertTrue(value6 == cache.get(key6));
	}
	
	@Test
	public void testLRU() {
		_strategy = IKVServer.CacheStrategy.LRU;
		_cacheSize = 3;
		cache = new Cache(_cacheSize,_strategy);

		Exception ex = null;
		String key1 = "Key1";
		String value1 = "Value1";
		String key2 = "Key2";
		String value2 = "Value2";
		String key3 = "Key3";
		String value3 = "Value3";
		String key4 = "Key4";
		String value4 = "Value4";
		String key5 = "Key5";
		String value5 = "Value5";
		String key6 = "Key6";
		String value6 = "Value6";

		cache.put(key1,value1);
		cache.put(key2,value2);
		cache.put(key3,value3);
		assertTrue(value1 == cache.get(key1));
		assertTrue(value2 == cache.get(key2));
		assertTrue(value3 == cache.get(key3));

		cache.get(key1);
		cache.put(key4,value4);
		assertTrue(value1 == cache.get(key1));
		assertTrue(null == cache.get(key2));
		assertTrue(value3 == cache.get(key3));
		assertTrue(value4 == cache.get(key4));

		cache.put(key5,value5);
		assertTrue(null == cache.get(key1));
		assertTrue(null == cache.get(key2));
		assertTrue(value3 == cache.get(key3));
		assertTrue(value4 == cache.get(key4));
		assertTrue(value5 == cache.get(key5));

		cache.get(key3);
		cache.get(key4);
		cache.put(key6,value6);
		assertTrue(null == cache.get(key1));
		assertTrue(null == cache.get(key2));
		assertTrue(value3 == cache.get(key3));
		assertTrue(value4 == cache.get(key4));
		assertTrue(null == cache.get(key5));
		assertTrue(value6 == cache.get(key6));
	}

	@Test
	public void testLFU() {
		_strategy = IKVServer.CacheStrategy.LFU;
		_cacheSize = 3;
		cache = new Cache(_cacheSize,_strategy);

		Exception ex = null;
		String key1 = "Key1";
		String value1 = "Value1";
		String key2 = "Key2";
		String value2 = "Value2";
		String key3 = "Key3";
		String value3 = "Value3";
		String key4 = "Key4";
		String value4 = "Value4";
		String key5 = "Key5";
		String value5 = "Value5";
		String key6 = "Key6";
		String value6 = "Value6";

		cache.put(key1,value1);
		cache.put(key2,value2);
		cache.put(key3,value3);
		assertTrue(value1 == cache.get(key1));
		assertTrue(value2 == cache.get(key2));
		assertTrue(value3 == cache.get(key3));

		cache.get(key1);
		cache.get(key1);
		cache.get(key1);
		cache.get(key1);
		cache.get(key1);
		cache.get(key3);
		cache.get(key3);
		cache.get(key2);
		cache.get(key2);
		cache.get(key2);

		cache.put(key4,value4);
		assertTrue(value1 == cache.get(key1));
		assertTrue(value2 == cache.get(key2));
		assertTrue(null == cache.get(key3));
		assertTrue(value4 == cache.get(key4));

		cache.get(key4);
		cache.get(key4);
		cache.get(key4);
		cache.get(key4);
		cache.get(key4);
		cache.get(key1);
		cache.get(key1);

		cache.put(key5,value5);
		assertTrue(value1 == cache.get(key1));
		assertTrue(null == cache.get(key2));
		assertTrue(null == cache.get(key3));
		assertTrue(value4 == cache.get(key4));
		assertTrue(value5 == cache.get(key5));

		cache.put(key6,value6);

		assertTrue(value1 == cache.get(key1));
		assertTrue(null == cache.get(key2));
		assertTrue(null == cache.get(key3));
		assertTrue(value4 == cache.get(key4));
		assertTrue(null == cache.get(key5));
		assertTrue(value6 == cache.get(key6));
	}
}