package shared;

public final class Utils {

    public static void validateKey(String key) throws IllegalArgumentException {
        if (key == null || key.length() == 0)
            throw new IllegalArgumentException("key");

        if (key.length() > 20)
            throw new IllegalArgumentException("key length must be <= 20");
    }

    public static void validateValue(String value) throws IllegalArgumentException {
        if (value == null)
            throw new IllegalArgumentException("value is null");
        if (value.length() > 120000)
            throw new IllegalArgumentException("value length must be <= 120000");
    }
}