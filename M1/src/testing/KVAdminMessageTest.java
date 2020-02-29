package testing;

import java.net.UnknownHostException;

import org.junit.Test;

import shared.serialization.*;
import shared.messages.*;

import junit.framework.TestCase;

public class KVAdminMessageTest extends TestCase {

    private boolean byteArrEq(byte[] a, byte[] b) {
        if (a == b)
            return true;

        if ((a == null) || (b == null))
            return false;

        if (a.length != b.length)
            return false;

        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i])
                return false;
        }

        return true;
    }

    @Test
    public void testConstructorValid() {
        byte[] value = {0x01, 0x02, 0x7f};

        /**
         * Correct KVAdminMessage creation
         */
        KVAdminMessage req = new KVAdminMessage(KVAdminMessage.StatusType.TRANSFER_REQUEST, value);

        assertTrue(req.getStatus() == KVAdminMessage.StatusType.TRANSFER_REQUEST);
        assertTrue(byteArrEq(req.getPayload(), value));

        try {
            KVAdminMessage req2 = KVAdminMessage.Deserialize(req.serialize());

            assertTrue(req2.getStatus() == req.getStatus());
            assertTrue(byteArrEq(req2.getPayload(), value));
        } catch (Deserializer.DeserializationException e1) {
            assertTrue(false);
        }

        /**
         * Construction with a null payload.
         */

        req = new KVAdminMessage(KVAdminMessage.StatusType.TRANSFER_REQUEST, null);
        assertTrue(byteArrEq(null, req.getPayload()));
        try {
            KVAdminMessage req3 = KVAdminMessage.Deserialize(req.serialize());

            assertTrue(byteArrEq(null, req3.getPayload()));
        } catch (Exception e){
            assertTrue(false);
        }
    }
}