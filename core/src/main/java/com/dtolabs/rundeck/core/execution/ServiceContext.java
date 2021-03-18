package com.dtolabs.rundeck.core.execution;

/**
 * Context about the service operation
 */
public interface ServiceContext<T> {
    /**
     *
     * @return true if successful
     */
    boolean isSuccessful();

    /**
     *
     * @return thrown exception
     */
    Throwable getThrowable();

    /**
     *
     * @return true if aborted
     */
    boolean isAborted();

    /**
     *
     * @return result object
     */
    T getResultObject();
}
