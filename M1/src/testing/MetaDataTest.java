package testing;

import org.junit.Test;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collection;

import shared.metadata.*;

public class MetaDataTest extends TestCase {

    @Test
    public void testHashValue() {
        String hashString1 = "00112233445566778899001122334455";
        String hashString2 = "00112233445566778899001122334456";
        String hashString3 = "FF112233445566778899001122334456";

        HashValue hv1 = HashValue.CreateFromHashString(hashString1);
        HashValue hv2 = HashValue.CreateFromHashString(hashString2);

        assertTrue(hv1.compareTo(hv2) < 0);
        assertTrue(hv2.compareTo(hv1) > 0);
        assertTrue(hv1.compareTo(hv1) == 0);

        // Serialization
        try {
            HashValue hv3 = HashValue.Deserialize(hv1.serialize());
            assertTrue(hv1.compareTo(hv3) == 0);

            HashValue hv4 = HashValue.CreateFromHashString(hashString3);
            HashValue hv5 = HashValue.Deserialize(hv4.serialize());
            assertTrue(hv5.compareTo(hv4) == 0);
        } catch (Exception e){
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void testHashRange() {
        String hashString1 = "00112233445566778899001122334455";
        String hashString2 = "00112233445566778899001122334456";
        String hashString3 = "00112233445566778899001122334457";

        HashValue hv1 = HashValue.CreateFromHashString(hashString1);
        HashValue hv2 = HashValue.CreateFromHashString(hashString2);
        HashValue hv3 = HashValue.CreateFromHashString(hashString3);

        assertTrue(hv1.equals(hv1));
        assertFalse(hv1.equals(hv2));
        assertFalse(hv1.equals(hv3));
        assertFalse(hv1.equals(null));

        HashRange hrA = new HashRange(hv1, hv2);
        assertTrue(hrA.isInRange(hv1));
        assertFalse(hrA.isInRange(hv2));
        assertFalse(hrA.isInRange(hv3));

        HashRange hrB = new HashRange(hv1, hv3);
        assertTrue(hrB.isInRange(hv1));
        assertTrue(hrB.isInRange(hv2));
        assertFalse(hrB.isInRange(hv3));

        HashRange hrC = new HashRange(hv2, hv3);
        assertFalse(hrC.isInRange(hv1));
        assertTrue(hrC.isInRange(hv2));
        assertFalse(hrC.isInRange(hv3));

        HashRange hrD = new HashRange(hv3, hv1);
        assertFalse(hrD.isInRange(hv1));
        assertFalse(hrD.isInRange(hv2));
        assertTrue(hrD.isInRange(hv3));

        HashRange hrE = new HashRange(hv3, hv2);
        assertTrue(hrE.isInRange(hv1));
        assertFalse(hrE.isInRange(hv2));
        assertTrue(hrE.isInRange(hv3));

        HashRange hrF = new HashRange(hv2, hv1);
        assertFalse(hrF.isInRange(hv1));
        assertTrue(hrF.isInRange(hv2));
        assertTrue(hrF.isInRange(hv3));

        HashRange hrG = new HashRange(hv1, hv1);
        assertTrue(hrG.isInRange(hv1));
        assertTrue(hrG.isInRange(hv2));
        assertTrue(hrG.isInRange(hv3));

        // Serialization
        try {
            HashRange hrA2 = HashRange.Deserialize(hrA.serialize());
            assertTrue(hrA2.isInRange(hv1));
            assertFalse(hrA2.isInRange(hv2));
            assertFalse(hrA2.isInRange(hv3));
        } catch (Exception e){
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void testMetaData() {
        String name = "name";
        String host = "host";
        int port = 420;
        String startHash = "00112233445566778899001122334455";
        String endHash = "00112233445566778899001122334456";

        HashRange hr1 = new HashRange(
            HashValue.CreateFromHashString(startHash),
            HashValue.CreateFromHashString(endHash)
        );

        MetaData md1 = new MetaData(name, host, port, startHash, endHash);
        assertTrue(md1.getName().equals(name));
        assertTrue(md1.getHost().equals(host));
        assertTrue(md1.getPort() == port);
        assertTrue(md1.getHashRange().equals(hr1));

        // Serialization
        try {
            MetaData md2 = MetaData.Deserialize(md1.serialize());
            assertTrue(md2.getName().equals(name));
            assertTrue(md2.getHost().equals(host));
            assertTrue(md2.getPort() == port);
            assertTrue(md2.getHashRange().equals(hr1));
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }

        // TODO TODO TODO: tsest getServerStateType
    }

    @Test
    public void testMetaDataSet() {
        Collection<ServerInfo> serverInfos = Arrays.asList(
            new ServerInfo("node1", "localhost", 1),
            new ServerInfo("node2", "localhost", 2),
            new ServerInfo("node3", "localhost", 3),
            new ServerInfo("node4", "localhost", 4)
        );

        MetaDataSet mds = MetaDataSet.CreateFromServerInfo(serverInfos);
        
        // Random hash values
        Collection<HashValue> inputs = Arrays.asList(
            HashValue.CreateFromHashString("00112233445566778899001122334455"),
            HashValue.CreateFromHashString("5011223344556677889988CC82334455"),
            HashValue.CreateFromHashString("8011223344123327889988CC82334455"),
            HashValue.CreateFromHashString("6011223344123327889988CC82334455")
        );

        for (HashValue input : inputs) {
            MetaData m = mds.getServerForHash(input);
            System.out.println(input + "," + m.getName());
            boolean matches = false;
            for (ServerInfo s : serverInfos) {
                if (s.getName().equals(m.getName()))
                    matches = true;
            }
            assertTrue(matches);
        }
    }
}