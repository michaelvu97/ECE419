package testing;

import java.net.UnknownHostException;

import org.junit.Test;

import shared.serialization.*;

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

        Exception e = null;
        try {
            d.getByte();
        } catch (Deserializer.DeserializationException dse) {
            e = dse;
        }

        assertNotNull(e);
    }
}

