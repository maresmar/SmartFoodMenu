package cz.maresmar.sfm.plugin.controller;

/**
 * Exception thrown when {@link ObjectHandler} tries to save object that is not annotated
 */
class UnsupportedTypeException extends RuntimeException {
    /**
     * Creates exception without message
     */
    public UnsupportedTypeException() {
    }

    /**
     * Creates exception with error message
     *
     * @param message Error message
     */
    public UnsupportedTypeException(String message) {
        super(message);
    }
}
