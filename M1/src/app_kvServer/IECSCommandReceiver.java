package app_kvServer;

/**
 * A connection which receieves commands from the ECS.
 */
public interface IECSCommandReceiver {

    /**
     * Establishes a connection to the ECS.
     */
    public void connect();
}