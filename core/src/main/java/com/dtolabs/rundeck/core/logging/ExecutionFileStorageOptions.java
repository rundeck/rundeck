package com.dtolabs.rundeck.core.logging;

/**
 * Define whether operations are supported
 */
public interface ExecutionFileStorageOptions {
    /**
     * @return true if retrieve is supported, false otherwise
     */
    boolean getRetrieveSupported();

    /**
     * @return true if store is supported, false otherwise
     */
    boolean getStoreSupported();
}
