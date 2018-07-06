package cz.maresmar.sfm.plugin.service;

/**
 * Parent of all plugin exceptions, these exceptions are send as extras back to the app
 *
 * @see cz.maresmar.sfm.plugin.BroadcastContract
 */
public class FoodPluginException extends RuntimeException {

    /**
     * Creates new exception
     *
     * @param msg       Exception message
     * @param throwable Source {@link Throwable}
     */
    FoodPluginException(String msg, Throwable throwable) {
        super(msg, throwable);
    }

    /**
     * Creates new exception
     *
     * @param msg Exception message
     */
    FoodPluginException(String msg) {
        super(msg);
    }

    /**
     * Creates new exception without message
     */
    FoodPluginException() {
        super();
    }
}
