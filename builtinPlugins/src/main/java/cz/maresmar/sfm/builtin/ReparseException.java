package cz.maresmar.sfm.builtin;

/**
 * Thrown when plugin needs to reparse the whole page from start
 */
public class ReparseException extends RuntimeException {
    /**
     * Create new exception with specified message
     *
     * @param msg Exception message
     */
    public ReparseException(String msg) {
        super(msg);
    }
}
