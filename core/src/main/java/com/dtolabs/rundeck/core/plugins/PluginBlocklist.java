package com.dtolabs.rundeck.core.plugins;

import java.util.List;
import java.util.Map;

public interface PluginBlocklist {

    public Boolean isPluginFilePresent(String fileName);

    public Boolean isPluginProviderPresent(String service, String providerName);

    public Boolean isBlocklistSet();
}


