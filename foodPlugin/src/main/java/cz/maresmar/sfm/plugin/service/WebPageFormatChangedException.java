package cz.maresmar.sfm.plugin.service;

/**
 * Exception thrown when plugin cannot read the web page. This usually happens when web page changes
 * format or if there is some captive portal blocking Internet
 */
public class WebPageFormatChangedException extends FoodPluginException {

    /**
     * Creates new exception without message
     */
    public WebPageFormatChangedException() {
        super();
    }

    /**
     * Creates new exception
     *
     * @param msg Exception message
     */
    public WebPageFormatChangedException(String msg) {
        super(msg);
    }

    /**
     * Creates new exception
     *
     * @param msg       Exception message
     * @param e         Source {@link Exception}
     */
    public WebPageFormatChangedException(String msg, Exception e) {
        super(msg, e);
    }
}
