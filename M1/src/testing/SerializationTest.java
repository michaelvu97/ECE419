package testing;

import java.net.UnknownHostException;

import org.junit.Test;

import shared.serialization.*;
import shared.metadata.*;
import shared.messages.*;

import junit.framework.TestCase;


public class SerializationTest extends TestCase {

    @Test
    public void testBasic() {
        int integer = 4;
        String s = "abcde1234";
        int integer2 = 6;
        byte b = (byte) 0xf0;

        byte[] bytes1 = {(byte)0xff, (byte) 0x00, (byte) 0x11};

        byte[] bytes = new Serializer()
            .writeInt(integer)
            .writeString(s)
            .writeInt(integer2)
            .writeByte(b)
            .writeBytes(bytes1)
            .writeString(null)
            .toByteArray();

        Deserializer d = new Deserializer(bytes);

        try {
            assertTrue(d.getInt() == integer);
            assertTrue(d.getString().equals(s));
            assertTrue(d.getInt() == integer2);
            assertTrue(d.getByte() == b);

            byte[] bytes2 = d.getBytes();
            assertTrue(bytes1[0]  == bytes2[0]);            
            assertTrue(bytes1[1]  == bytes2[1]);
            assertTrue(bytes1[2]  == bytes2[2]);

            assertTrue(d.getString() == null);
        } catch (Exception e1) {
            e1.printStackTrace();
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

    @Test
    public void testObjSerialization() {
        HashValue hashValue = HashValue.CreateFromHashString("01234567890123456789012345678901");

        byte[] bytes = new Serializer()
            .writeObject(hashValue)
            .toByteArray();

        Deserializer d = new Deserializer(bytes);

        try {
            HashValue hashValue2 = HashValue.Deserialize(d.getBytes());
            assertTrue(hashValue2.compareTo(hashValue) == 0);
        } catch (Exception e){
            assertTrue(false);
        }     
    }
}

