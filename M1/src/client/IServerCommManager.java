package client;

import java.io.IOException;

import shared.messages.*;
import shared.serialization.*;

public interface IServerCommManager {
    /**
     * Sends a KVMessage to the KVServer cloud.
     * Internally, will handle SERVER_NOT_RESPONSIBLE responses, and will retry
     * and refresh metadata until the request is fulfilled.
     *
     * Will not return KVMessages of type: {
     *  SERVER_STOPPED,
     *  SERVER_WRITE_LOCK,
     *  SERVER_NOT_RESPONSIBLE
     * }
     */
    public KVMessage sendRequest(KVMessage message)
            throws Deserializer.DeserializationException, IOException;
}