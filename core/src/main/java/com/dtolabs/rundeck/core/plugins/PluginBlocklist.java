package com.dtolabs.rundeck.core.plugins;

/**
 * Matches plugin file or providers against a block list
 */
public interface PluginBlocklist {

    /**
     * @return true if file name is present
     */
    public boolean isPluginFilePresent(String fileName);

    /**
     * @return true if provider name and service are present in the list
     */
    public boolean isPluginProviderPresent(String service, String providerName);
}


