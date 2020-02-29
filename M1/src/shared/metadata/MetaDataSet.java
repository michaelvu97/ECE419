package shared.metadata;

import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;

import shared.serialization.*;

/**
 * Immutable meta data set object.
 * Modificiations are made through functional methods.
 */
public final class MetaDataSet implements ISerializable {
    private MetaData[] _data = new MetaData[0];

    public MetaDataSet(Collection<MetaData> items) {
        if (items == null)
            throw new IllegalArgumentException("items is null");

        if (true) {
            ArrayList<MetaData> data = new ArrayList<MetaData>(items);
            Collections.sort(data);
            _data = data.toArray(_data);
        }

        /**
         *
         * Validation
         * (still requires unit tests)
         *
         */

        // These are currently based on best guesses of behaviour.
        if (_data.length == 0) 
            // TODO this case might not be valid
            throw new IllegalArgumentException("items are empty");

        // If only one entry, verify that it is encompassing.
        if (_data.length == 1 && !_data[0].getHashRange().getIsEncompassing()) {
            throw new IllegalArgumentException(
                "set with only entry is not encompassing"
            );
        }

        // If many entries, verify that none are encompassing.
        if (_data.length != 1) {
            for (int i = 0; i < _data.length; i++) {
                if (_data[i].getHashRange().getIsEncompassing()) {
                    throw new IllegalArgumentException(
                        "MetaData set contains encompassing element: [" + i 
                        + "]"
                    );
                }
            }
        }

        // Confirm that each hash ends where the next begins
        for (int i = 0; i < _data.length; i++) {
            if (_data[i].getHashRange().getEnd().compareTo(_data[(i + 1) % _data.length].getHashRange().getStart()) != 0) {
                throw new IllegalArgumentException(
                    "MetaData set does not cover the whole hash space. "
                    + "md:(" + _data[i].getHashRange().getEnd() + ") != md:(" 
                    + _data[(i + 1) % _data.length].getHashRange().getStart() + ")"
                );
            }
        }

        // Confirm that the last one wraps around
        if (!_data[_data.length - 1].getHashRange().getWrapsAround()) {
            throw new IllegalArgumentException(
                "Last metadata entry does not wrap around"
            );
        }

        // Confirm that no others wrap around.
        for (int i = 0; i < _data.length - 1; i++) {
            if (_data[i].getHashRange().getWrapsAround()) {
                throw new IllegalArgumentException(
                    "Non-last metadata entry wraps around: [" + i + "]");
            }
        }
    }

    /**
     * Constructs a metadata set from a collection of server infos.
     */
    public static MetaDataSet CreateFromServerInfo(
            Collection<ServerInfo> serverInfos) {
        
        ArrayList<ServerInfo> infoList = new ArrayList<ServerInfo>(serverInfos);
        Collections.sort(infoList);

        // Singular case
        if (infoList.size() == 0) {
            throw new IllegalArgumentException("No server infos provided");
        }

        ArrayList<MetaData> result = new ArrayList<MetaData>();

        for (int i = 0; i < infoList.size(); i++) {
            // added: server availability check.
            if (infoList.get(i).getAvailability()) {
                ServerInfo curr = infoList.get(i);
                ServerInfo next = infoList.get((i + 1) % infoList.size());

                result.add(
                    new MetaData(
                        curr.getName(),
                        curr.getHost(), 
                        curr.getPort(),
                        curr.getHash(),
                        next.getHash()
                    )
                );
            }
        }
        return new MetaDataSet(result);
    }

    public MetaData getServerForHash(HashValue hv) {
        if (hv == null)
            throw new IllegalArgumentException("hv is null");

        for (MetaData srv : _data) {
            if (srv.getHashRange().isInRange(hv))
                return srv;
        }

        // This should never happen.
        throw new IllegalStateException("Could not find server for hash " + hv);
    }

    public static MetaDataSet Deserialize(byte[] serializedBytes)
            throws Deserializer.DeserializationException {
        return new MetaDataSet(
            new Deserializer(serializedBytes)
                .getList(MetaData.Deserialize)
        );
    }

    @Override
    public byte[] serialize() {
        return new Serializer()
            .writeList(_data)
            .toByteArray();
    }
}