package com.dtolabs.rundeck.core.plugins;

import java.util.List;
import java.util.Map;

/**
 * Matches plugin file or providers against a block list
 */
public interface PluginBlocklist {

    /**
     * @return true if file name is present
     */
    public Boolean isPluginFilePresent(String fileName);

    /**
     * @return true if provider name and service are present in the list
     */
    public Boolean isPluginProviderPresent(String service, String providerName);

    public Boolean isBlocklistSet();
}


