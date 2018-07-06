package cz.maresmar.sfm.plugin.service;

/**
 * Exception used when the user provides wrong password
 */
public class WrongPassException extends FoodPluginException {

    /**
     * Creates new exception
     *
     * @param msg Exception message
     */
    public WrongPassException(String msg) {
        super(msg);
    }

    /**
     * Creates new exception without message
     */
    public WrongPassException() {
        super();
    }
}
