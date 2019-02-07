package cz.maresmar.sfm;

import androidx.annotation.NonNull;

/**
 * Debug testing class for assert checking
 */
public class Assert {

    // To prevent someone from accidentally instantiating the this class,
    // make the constructor private.
    private Assert() {
    }

    /**
     * Check condition
     *
     * @param condition  Condition to be true
     * @param errMessage Error message
     * @param args       Error message args
     * @see String#format(String, Object...)
     */
    public static void that(boolean condition, @NonNull String errMessage, Object... args) {
        if (!condition) {
            throw new AssertionError(String.format(errMessage, args));
        }
    }

    /**
     * Check if number is zero
     *
     * @param number Number to be zero
     */
    public static void isZero(int number) {
        if (number != 0) {
            throw new AssertionError("Expected 0 but found " + number);
        }
    }

    /**
     * Check if number is one
     *
     * @param number Number to be one
     */
    public static void isOne(int number) {
        if (number != 1) {
            throw new AssertionError("Expected 1 but found " + number);
        }
    }

    /**
     * Throws error message with given message
     *
     * @param msg  Message to be shown
     * @param args Message arguments
     * @see String#format(String, Object...)
     */
    public static void fail(@NonNull String msg, Object... args) {
        throw new AssertionError(String.format(msg, args));
    }
}
