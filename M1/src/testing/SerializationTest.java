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
    public void testClientRequests() {
        String key = "this-is-a-key";
        String value = "this-is-the-value";

        /**
         * Correct client request creation
         */
        KVMessage putReq = new KVMessageImpl(KVMessage.StatusType.PUT, key, value);

        assertTrue(putReq.getStatus() == KVMessage.StatusType.PUT);
        assertTrue(putReq.getKey().equals(key));
        assertTrue(putReq.getValue().equals(value));

        try {
            KVMessage m2 = KVMessageImpl.Deserialize(putReq.serialize());

            assertTrue(m2.getStatus() == putReq.getStatus());
            assertTrue(m2.getKey() == putReq.getKey());
            assertTrue(m2.getValue() == putReq.getValue());
        } catch (Exception e1) {
            assertTrue(false);
        }

        KVMessage getReq = new KVMessageImpl(KVMessage.StatusType.GET, key, null);

        assertTrue(getReq.getStatus() == KVMessage.StatusType.PUT);
        assertTrue(getReq.getKey().equals(key));
        assertTrue(getReq.getValue() == null);

        try {
            KVMessage m3 = KVMessageImpl.Deserialize(getReq.serialize());

            assertTrue(m3.getStatus() == getReq.getStatus());
            assertTrue(m3.getKey() == getReq.getKey());
            assertTrue(m3.getValue() == getReq.getValue());
        } catch (Exception e1) {
            assertTrue(false);
        }

    }
}

