package shared;

public final class ZooKeeperConstants {
    
    // Not allowed
    private ZooKeeperConstants(){
        throw new IllegalStateException();
    }

    public final static String APP_FOLDER = "/kvclients";

    public final static String ZK_PORT = "2181";

    // Measured in ms.
    public final static int TIMEOUT = 1500;
}