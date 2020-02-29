package shared.metadata;

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
        _hash = HashUtil.ComputeHash(_host, _port);
        available = false;
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