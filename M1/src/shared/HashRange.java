package shared;

public final class HashRange {
    private String _hashStart;
    private String _hashEnd;

    public HashRange(String start, String end) {
        if (start == null)
            throw new IllegalArgumentException("start");
        if (end == null)
            throw new IllegalArgumentException("end");
        _hashStart = start;
        _hashEnd = end;
    }
}