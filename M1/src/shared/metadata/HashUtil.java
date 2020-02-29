package shared.metadata;

import java.security.MessageDigest;

public final class HashUtil {
    private HashUtil() {

    }

    public static String ComputeHash(String hostname, int port) {
        String combinedStr = hostname + ":" + port;

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            // Swallow exception, this won't happen.
        }

        md.update(combinedStr.getBytes());
        byte[] result = md.digest();
        
        return HashValue.CreateFromMD5Bytes(result).toString();
    }

    public static String ComputeHash(String key) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            // Swallow exception, this won't happen.
        }

        md.update(key.getBytes());
        byte[] result = md.digest();
        
        return HashValue.CreateFromMD5Bytes(result).toString();
    }
}