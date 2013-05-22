package rundeck.services.logging

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 5/23/13
 * Time: 1:39 PM
 */
public enum LogState {
    /**
     * Not found at all
     */
    NOT_FOUND,
    /**
     * Present locally
     */
    FOUND_LOCAL,
    /**
     * Present on remote storage
     */
    FOUND_REMOTE,
    /**
     * Pending presence on remote storage
     */
    PENDING_REMOTE,
    /**
     * Pending presence on local storage (being copied)
     */
    PENDING_LOCAL
}
