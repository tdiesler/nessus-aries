package io.nessus.aries.util;

/**
 * Legal state assertions
 *
 */
public final class AssertState {

    // hide ctor
    private AssertState() {
    }

    /**
     * Throws an IllegalStateException when the given value is not null.
     * @return the value
     */
    public static <T> T isNull(T value) {
        return isNull(value, "Not null: " + value);
    }

    /**
     * Throws an IllegalStateException when the given value is not null.
     * @return the value
     */
    public static <T> T isNull(T value, String message) {
        if (value != null)
            throw new IllegalStateException(message);
        return value;
    }

    /**
     * Throws an IllegalStateException when the given value is null.
     * @return the value
     */
    public static <T> T notNull(T value) {
        return notNull(value, "Null value");
    }

    /**
     * Throws an IllegalStateException when the given value is null.
     * @return the value
     */
    public static <T> T notNull(T value, String message) {
        if (value == null)
            throw new IllegalStateException(message);
        return value;
    }

    /**
     * Throws an IllegalStateException when the given value is not true.
     */
    public static Boolean isTrue(Boolean value) {
        return isTrue(value, "Not true");
    }

    /**
     * Throws an IllegalStateException when the given value is not true.
     */
    public static Boolean isTrue(Boolean value, String message) {
        if (!Boolean.valueOf(value))
            throw new IllegalStateException(message);

        return value;
    }

    /**
     * Throws an IllegalStateException when the given value is not false.
     */
    public static Boolean isFalse(Boolean value) {
        return isFalse(value, "Not false");
    }

    /**
     * Throws an IllegalStateException when the given value is not false.
     */
    public static Boolean isFalse(Boolean value, String message) {
        if (Boolean.valueOf(value))
            throw new IllegalStateException(message);
        return value;
    }

    /**
     * Throws an IllegalStateException when the given values are not equal.
     */
    public static <T> T isEqual(T exp, T was) {
        return isEqual(exp, was, exp + " != " + was);
    }

    /**
     * Throws an IllegalStateException when the given values are not equal.
     */
    public static <T> T isEqual(T exp, T was, String message) {
        notNull(exp, message);
        notNull(was, message);
        isTrue(exp.equals(was), message);
        return was;
    }

    /**
     * Throws an IllegalStateException when the given values are not the same.
     */
    public static <T> T isSame(T exp, T was) {
        return isSame(exp, was, exp + " != " + was);
    }

    /**
     * Throws an IllegalStateException when the given values are not the same.
     */
    public static <T> T isSame(T exp, T was, String message) {
        notNull(exp, message);
        notNull(was, message);
        isTrue(exp == was, message);
        return was;
    }
}
