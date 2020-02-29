package app_kvServer;

import shared.messages.KVAdminMessage;

public final class ECSCommandReceiver implements IECSCommandReceiver {

    private IKVServer _kvServer;

    /**
     * Connects on construction.
     */
    public ECSCommandReceiver(IKVServer kvServer) {
        if (kvServer == null)
            throw new IllegalArgumentException("kvServer is null");

        _kvServer = kvServer;

        connect();
    }

    public void connect() {
        // TODO
    }

    /**
     * Called when a transfer request is receieved.
     */
    public void onTransferRequest(KVAdminMessage transferRequest) {
        // TODO
    }

    /**
     * Called when an update metadata request is received.
     */
    public void onUpdateMetadataRequest(KVAdminMessage updateMetadataRequest) {
        // Lock the server
        // Delete any data that is no longer owned by this server
        // Update the metadata
        // Unlock the server
    }


}