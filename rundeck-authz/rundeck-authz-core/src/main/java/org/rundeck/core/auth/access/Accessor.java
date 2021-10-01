package org.rundeck.core.auth.access;

/**
 * Accessor of an authorized resource of a given Type
 *
 * @param <T> resource type
 */
public interface Accessor<T> {
    /**
     * @return resource
     * @throws UnauthorizedAccess if unauthorized
     * @throws NotFound           if not found
     */
    T getAccess() throws UnauthorizedAccess, NotFound;

    /**
     * @return true if the resource exists
     */
    boolean isExists();

    /**
     * @return true if accessible
     */
    boolean isAllowed() throws NotFound;
}
