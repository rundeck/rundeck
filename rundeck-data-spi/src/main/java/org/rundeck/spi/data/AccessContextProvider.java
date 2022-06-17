package org.rundeck.spi.data;

/**
 * Provides implicit context to data access requests
 *
 * @param <T>
 */
public interface AccessContextProvider<T> {
    T getContext();
}
