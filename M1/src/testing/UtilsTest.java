package testing;

import java.net.UnknownHostException;

import org.junit.Test;

import shared.Utils;
import junit.framework.TestCase;


public class UtilsTest extends TestCase {

    @Test
    public void testBasic() {
        assertTrue(Utils.containsNewline("abcd\nadedf"));
        assertTrue(Utils.containsNewline("\n"));
        assertTrue(Utils.containsNewline("\nasfsdf"));

        assertFalse(Utils.containsNewline(""));
        assertFalse(Utils.containsNewline("abc"));
        assertFalse(Utils.containsNewline("abcasdfasd sdfsdf"));
    }
}

