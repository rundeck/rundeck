package org.rundeck.core.auth.access;

/**
 * Accessor of an authorized resource of a given Type
 *
 * @param <T> resource type
 */
public interface Accessor<T> {
    /**
     * @return resource if authorized and it exists
     * @throws UnauthorizedAccess if unauthorized
     * @throws NotFound           if not found
     */
    T getResource() throws UnauthorizedAccess, NotFound;

    /**
     * @param accessDescription a description of access requested
     * @return resource if authorized and it exists
     * @throws UnauthorizedAccess if unauthorized
     * @throws NotFound           if not found
     */
    T getResource(String accessDescription) throws UnauthorizedAccess, NotFound;

    /**
     * check authorization and existence only
     *
     * @throws UnauthorizedAccess if unauthorized
     * @throws NotFound           if not found
     */
    void authorize() throws UnauthorizedAccess, NotFound;

    /**
     * check authorization and existence only
     *
     * @param accessDescription a description of access requested
     * @throws UnauthorizedAccess if unauthorized
     * @throws NotFound           if not found
     */
    void authorize(String accessDescription) throws UnauthorizedAccess, NotFound;

    /**
     * @return true if the resource exists
     */
    boolean isExists();

    /**
     * @return true if accessible
     */
    boolean isAllowed() throws NotFound;
}
