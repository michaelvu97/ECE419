package shared.metadata;

import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Iterator;

import shared.serialization.*;

import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Immutable meta data set object.
 * Modificiations are made through functional methods.
 */
public final class MetaDataSet implements ISerializable, Iterable<MetaData> {
    private MetaData[] _data = new MetaData[0];

    private class MetaDataSetIterator implements Iterator<MetaData> {

        private MetaData[] _data;
        private int length;
        private int index = 0;

        public MetaDataSetIterator(MetaData[] data) {
            _data = data;
            length = data.length;
        }

        @Override
        public boolean hasNext() {
            return index < length;
        }

        @Override 
        public MetaData next() {
            return _data[index++];
        }

        @Override
        public void remove() {
            throw new IllegalStateException("Remove not allowed");
        }
    }

    private static Logger logger = Logger.getRootLogger();

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

    @Override
    public Iterator<MetaData> iterator() {
        return new MetaDataSetIterator(_data);
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
        return new MetaDataSet(result);
    }

    public MetaData getPrimaryForHash(HashValue hv) {
        return getReplicaForHash(hv, 0);
    }

    public MetaData getReplicaForHash(HashValue hv, int replica_num) {
        if (hv == null)
            throw new IllegalArgumentException("hv is null");

        if (replica_num > 2 || replica_num < 0) {
            throw new IllegalArgumentException("replica_num is out of range: " 
                    + replica_num);
        }

        if (replica_num == 0)
            logger.debug("Finding primary for " + hv);
        else
            logger.debug("Finding replica for " + hv);

        for (int i = 0; i < _data.length; i++) {
            if (!_data[i].getHashRange().isInRange(hv))
                continue;

            // Note: this is so that replicas come before the primary
            int data_index = (i - replica_num) % _data.length;
            if (data_index < 0)
                data_index +=_data.length; 

            logger.debug("Found server " + _data[data_index].getName());
            return _data[data_index];
        }

        throw new IllegalStateException("Cound not find server for hash: " + 
                hv + ", replica=" + replica_num);
    }

    public boolean inReplicaRange(HashValue hv, MetaData cur_server){
        if(cur_server.getName().equals(getReplicaForHash(hv,1).getName())
            || cur_server.getName().equals(getReplicaForHash(hv,2).getName())){
                return true;
        } else {
            return false;
        }
    }

    public MetaData getMetaDataByName(String name) {
        for (MetaData srv : _data) {
            if (srv.getName().equals(name))
                return srv;
        }

        return null;
    }

    public static MetaDataSet Deserialize(byte[] serializedBytes)
            throws Deserializer.DeserializationException {
        return new MetaDataSet(
            new Deserializer(serializedBytes)
                .getList(MetaData.Deserialize)
        );
    }

    @Override
    public String toString() {
        String result = "[";
        for (MetaData m : _data) {
            result += m.toString() + ", ";
        }
        result += "]";
        return result;
    }

    @Override
    public byte[] serialize() {
        return new Serializer()
            .writeList(_data)
            .toByteArray();
    }
}