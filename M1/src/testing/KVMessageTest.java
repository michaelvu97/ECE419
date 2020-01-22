package testing;

import java.net.UnknownHostException;

import org.junit.Test;

import shared.Serializer;
import shared.Deserializer;

import shared.messages.*;

import junit.framework.TestCase;

public class KVMessageTest extends TestCase {

    @Test
    public void testConstructorValid() {
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
            assertTrue(m2.getKey().equals(putReq.getKey()));
            assertTrue(m2.getValue().equals(putReq.getValue()));
        } catch (Deserializer.DeserializationException e1) {
            assertTrue(false);
        }

        KVMessage getReq = new KVMessageImpl(KVMessage.StatusType.GET, key, null);

        assertTrue(getReq.getStatus() == KVMessage.StatusType.GET);
        assertTrue(getReq.getKey().equals(key));
        assertTrue(getReq.getValue() == null);

        try {
            KVMessage m3 = KVMessageImpl.Deserialize(getReq.serialize());

            assertTrue(m3.getStatus() == getReq.getStatus());
            assertTrue(m3.getKey().equals(getReq.getKey()));
            assertTrue(m3.getValue() == null);
        } catch (Exception e1) {
            assertTrue(false);
        }

        // All server responses
        for (KVMessage.StatusType type : KVMessage.StatusType.values()) {
            if (type == KVMessage.StatusType.GET || 
                type == KVMessage.StatusType.PUT) {
                continue;
            }

            KVMessage serverResponse = new KVMessageImpl(type, key, value);

            assertTrue(serverResponse.getStatus() == type);
            assertTrue(serverResponse.getKey().equals(key));
            assertTrue(serverResponse.getValue().equals(value));

            try {
                KVMessage m4 = KVMessageImpl.Deserialize(serverResponse.serialize());

                assertTrue(m4.getStatus() == type);
                assertTrue(m4.getKey().equals(key));
                assertTrue(m4.getValue().equals(value));

            } catch (Exception e) {
                assertTrue(false);
            }
        }
    }

    @Test
    public void testConstructorExceptions() {

        String key = "key";
        String value = "value";

        Exception e = null;

        // GET value        
        try {
            KVMessage req = new KVMessageImpl(KVMessage.StatusType.GET, key, value);
        } catch (Exception e1){
            e = e1;
        }

        assertTrue(e != null);
        e = null;

        // GET key
        try {
            KVMessage req = new KVMessageImpl(KVMessage.StatusType.GET, null, null);
        } catch (Exception e1) {
            e = e1;
        }
        assertTrue(e != null);
        e = null;

        // PUT key
        try {
            KVMessage req = new KVMessageImpl(KVMessage.StatusType.PUT, null, null);
        } catch (Exception e1) {
            e = e1;
        }
        assertTrue(e != null);
        e = null;

        try {
            KVMessage req = new KVMessageImpl(KVMessage.StatusType.PUT, null, value);
        } catch (Exception e1) {
            e = e1;
        }
        assertTrue(e != null);
        e = null;
    }

}