package shared.serialization;

import java.util.*;

public class Serializer {

    private ArrayList<Byte> _byteList = new ArrayList<Byte>();

    public byte[] toByteArray() {
        byte[] result = new byte[_byteList.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = _byteList.get(i).byteValue();
        }
        return result;
    }

    public Serializer writeInt(int num) {
        writeByte((byte) (num >> 24));
        writeByte((byte) (num >> 16));
        writeByte((byte) (num >> 8));
        writeByte((byte) (num));

        return this;
    }

    public Serializer writeByte(byte b){
        _byteList.add(new Byte(b));
        return this;
    }

    // Allows null
    public Serializer writeObject(ISerializable obj) {
        if (obj == null) {
            writeInt(0);
            return this;
        }

        writeBytes(obj.serialize());
        return this;
    }

    // Allows null
    public Serializer writeBytes(byte[] b) {
        if (b == null)
        {
            writeInt(0);
            return this;
        }

        writeInt(b.length);
        for (byte cur_b : b) {
            writeByte(cur_b);
        }

        return this;
    }

    public Serializer writeString(String str) {
        if (str == null)
        {
            writeInt(0);
            return this;
        }

        writeInt(str.length());

        for (char c : str.toCharArray()) {
            writeByte((byte) c);
        }

        return this;
    }
}