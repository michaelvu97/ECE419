package shared;

import java.util.*;

public class Serializer {
    private ArrayList<Byte> _byteList = new ArrayList<Byte>();

    public Serializer() {

    }

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

    public Serializer writeString(String str) {
        if (str == null)
            throw new IllegalArgumentException("str is null");

        writeInt(str.length());

        for (char c : str.toCharArray()) {
            writeByte((byte) c);
        }

        return this;
    }
}