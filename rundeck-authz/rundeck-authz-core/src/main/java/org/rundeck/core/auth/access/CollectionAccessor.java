package org.rundeck.core.auth.access;

import java.util.List;

/**
 * Authorized resource accessor for a collection of resources
 *
 * @param <T>  resource type
 * @param <ID> ID type
 */
public interface CollectionAccessor<T, ID> {

    /**
     * @return all found and allowed resources
     */
    List<T> getFiltered();

    /**
     * @return all resources
     * @throws UnauthorizedAccess if any resource is unauthorized
     * @throws NotFound           if any resource is not found
     */
    List<T> getAccess() throws UnauthorizedAccess, NotFound;

    /**
     * @return all found resources
     * @throws UnauthorizedAccess if any resource is unauthorized
     */
    List<T> getFound() throws UnauthorizedAccess;

    /**
     * @return all found and allowed IDs
     */
    List<ID> getFilteredIds();

    /**
     * @return all found IDs
     * @throws UnauthorizedAccess if any resource is unauthorized
     */
    List<ID> getFoundIds() throws UnauthorizedAccess;

    /**
     * @return true if all input resources were allowed
     */
    boolean isAllAllowed();

    /**
     * @return true if all input resources were found
     */
    boolean isAllFound();
}
