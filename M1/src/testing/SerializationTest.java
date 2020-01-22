package testing;

import java.net.UnknownHostException;

import org.junit.Test;

import shared.Serializer;
import shared.Deserializer;

import shared.messages.*;

import junit.framework.TestCase;


public class SerializationTest extends TestCase {

    @Test
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
            .writeString(null)
            .toByteArray();

        Deserializer d = new Deserializer(bytes);

        try {
            assertTrue(d.getInt() == integer);
            assertTrue(d.getString().equals(s));
            assertTrue(d.getInt() == integer2);
            assertTrue(d.getByte() == b);
            assertTrue(d.getString() == null);
        } catch (Exception e1) {
            assertTrue(false);
        }
    }

    @Test
    public void testClientGetMessage () {
        String key = "this is a key";
        KVMessage m = KVClientRequestMessage.GET(key);
        byte[] bytes = m.serialize();
        try {
            KVMessage m2 = KVClientRequestMessage.Deserialize(bytes);
            assertTrue(m2 != null);
            assertTrue(m2.getStatus() == KVMessage.StatusType.GET);
            assertTrue(m2.getKey().equals(key));
        } catch (Exception e) {
            assertTrue(false);
        }
    }
    
        @Test
    public void testClientPutMessage () {
        String key = "this is a key";
        String value = "this is a value";
        KVMessage m = KVClientRequestMessage.PUT(key, value);
        byte[] bytes = m.serialize();
        try {
            KVMessage m2 = KVClientRequestMessage.Deserialize(bytes);
            assertTrue(m2 != null);
            assertTrue(m2.getStatus() == KVMessage.StatusType.PUT);
            assertTrue(m2.getKey().equals(key));
            assertTrue(m2.getValue().equals(value));
        } catch (Exception e) {
            assertTrue(false);
        }
    }

    @Test
    public void testServerResponseMessage() {
        KVMessage.StatusType type = KVMessage.StatusType.PUT_SUCCESS;
        String responseKey = "hello world!";
        String responseValue = " value ";

        KVServerResponseMessage responseObj = new KVServerResponseMessage(type, responseKey, responseValue);
        byte[] bytes = responseObj.serialize();
        try {
            KVMessage reconstructed = KVServerResponseMessage.Deserialize(bytes);
            assertTrue(reconstructed.getStatus() == type);
            assertTrue(reconstructed.getKey().equals(responseKey));
            assertTrue(reconstructed.getValue().equals(responseValue));
        } catch (shared.Deserializer.DeserializationException dse) {
            assertTrue(false);
        }
    }

}

