package app_kvECS;

import shared.metadata.*;
import shared.serialization.*;

public final class TransferRequest implements ISerializable {

    private String _fromName, _toName;
    private MetaDataSet _newMetaDataSet;

    public TransferRequest(String fromName, String toName, 
            MetaDataSet newMetaDataSet) {

        if (fromName == null || fromName.length() == 0)
            throw new IllegalArgumentException("from name is empty/null");
        if (toName == null || toName.length() == 0)
            throw new IllegalArgumentException("to name is empty/null");
        if (newMetaDataSet == null)
            throw new IllegalArgumentException("metadataset is null");

        _fromName = fromName;
        _toName = toName;
        _newMetaDataSet = newMetaDataSet;
    }

    public String getFromName() { return _fromName; }
    public String getToName() { return _toName; }
    public MetaDataSet getNewMetaDataset() { return _newMetaDataSet; }

    public byte[] serialize() {
        return new Serializer()
            .writeString(_fromName)
            .writeString(_toName)
            .writeObject(_newMetaDataSet)
            .toByteArray();
    }

    public static TransferRequest Deserialize(byte[] serializedBytes)
            throws Deserializer.DeserializationException {
        if (serializedBytes == null)
            throw new IllegalArgumentException("serializedBytes is null");

        Deserializer d = new Deserializer(serializedBytes);

        return new TransferRequest(
            d.getString(),
            d.getString(),
            MetaDataSet.Deserialize(d.getObjectBytes())
        );
    }

}