package cz.maresmar.sfm.plugin.service;

/**
 * Exception thrown when server is temporally inaccessible (because of maintenance etc...)
 */
public class ServerMaintainException extends FoodPluginException {
    /**
     * Creates new exception without message
     */
    public ServerMaintainException() {
        super();
    }

    /**
     * Creates new exception
     *
     * @param msg Exception message
     */
    public ServerMaintainException(String msg) {
        super(msg);
    }
}
