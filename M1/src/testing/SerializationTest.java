package testing;

import java.net.UnknownHostException;

import shared.Serializer;
import shared.Deserializer;

import shared.messages.*;
import shared.messages.KVClientRequestMessage.RequestType;

import junit.framework.TestCase;


public class SerializationTest extends TestCase {

    public void testBasic() {
        int integer = 4;
        String s = "abcde1234";
        int integer2 = 6;
        byte b = (byte) 0xf0;

        byte[] bytes = new Serializer()
            .writeInt(integer)
            .writeString(s)
            .writeInt(integer2)
            .writeByte(b)
            .toByteArray();

        Deserializer d = new Deserializer(bytes);

        assertTrue(d.getInt() == integer);
        assertTrue(d.getString().equals(s));
        assertTrue(d.getInt() == integer2);
        assertTrue(d.getByte() == b);
    }

    public void testClientGetMessage () {
        String key = "this is a key";
        KVGetMessage m = new KVGetMessage(key);
        byte[] bytes = m.convertToBytes();
        try {
            KVGetMessage m2 = (KVGetMessage) KVClientRequestMessage.Deserialize(bytes);
            assertTrue(m2 != null);
            assertTrue(m2.getType() == RequestType.GET);
            assertTrue(m2.getKey().equals(key));
        } catch (Exception e) {
            assertTrue(false);
        }
    }
    
    public void testClientPutMessage () {
        String key = "this is a key";
        String value = "this is a value";
        KVPutMessage m = new KVPutMessage(key, value);
        byte[] bytes = m.convertToBytes();
        try {
            KVPutMessage m2 = (KVPutMessage) KVClientRequestMessage.Deserialize(bytes);
            assertTrue(m2 != null);
            assertTrue(m2.getType() == RequestType.PUT);
            assertTrue(m2.getKey().equals(key));
            assertTrue(m2.getValue().equals(value));
        } catch (Exception e) {
            assertTrue(false);
        }
    }

}

