package org.rundeck.core.auth.access;

/**
 * Locates a resource without checking authorization
 *
 * @param <T> type
 */
public interface Locator<T> {
    /**
     * @return resource if it exists
     * @throws NotFound if not found
     */
    T getResource() throws NotFound;

    /**
     * @return true if the resource exists
     */
    boolean isExists();
}
