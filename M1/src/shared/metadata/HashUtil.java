package shared.metadata;

import java.security.MessageDigest;

public final class HashUtil {
    private HashUtil() {

    }

    public static String ComputeHashString(String hostname, int port) {
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

    public static HashValue ComputeHash(String hostname, int port) {
        return HashValue.CreateFromHashString(ComputeHashString(hostname, port));
    }

    public static String ComputeHashStringFromKey(String key) {
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

    public static HashValue ComputeHashFromKey(String key) {
        return HashValue.CreateFromHashString(ComputeHashStringFromKey(key));
    }
}