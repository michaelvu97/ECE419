package shared.metadata;

import java.security.MessageDigest;

public class ServerInfo implements Comparable<ServerInfo> {
    private String _name;
    private String _host;
    private int _port;
    private String _hash;
    private boolean available = true;

    public ServerInfo(String name, String host, int port) {
        if (name == null)
            throw new IllegalArgumentException("name is null");
        if (host == null)
            throw new IllegalArgumentException("host is null");
        if (port < 0)
            throw new IllegalArgumentException("port is negative: " + port);

        _name = name;
        _host = host;
        _port = port;
        _hash = ComputeHash(_host, _port);
        available = false;
    }

    private static String ComputeHash(String hostname, int port) {
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

    public boolean setAvailability(boolean availability) {
        available = availability;
        return available;
    }

    public boolean getAvailability() {
        return available;
    }

    public String getName() {
        return _name;
    }

    public String getHost() {
        return _host;
    }

    public int getPort() {
        return _port;
    }

    public String getHash() {
        return _hash;
    }

    @Override
    public int compareTo(ServerInfo other) {
        HashValue v1 = HashValue.CreateFromHashString(getHash());
        HashValue v2 = HashValue.CreateFromHashString(other.getHash());

        return v1.compareTo(v2);
    }
}