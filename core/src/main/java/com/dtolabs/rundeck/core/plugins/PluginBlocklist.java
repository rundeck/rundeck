package com.dtolabs.rundeck.core.plugins;

import java.util.List;
import java.util.Map;

public interface PluginBlocklist {

    public List<String> getBlockListPluginFileName();

    public Map<String,List<String>> getBlockListMap();

    public Boolean isBlacklistSet();
}


