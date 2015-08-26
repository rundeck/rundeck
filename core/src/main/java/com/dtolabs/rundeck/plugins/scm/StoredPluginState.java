package com.dtolabs.rundeck.plugins.scm;

import java.util.Map;

/**
 * Created by greg on 8/24/15.
 */
public interface StoredPluginState extends PluginState {
    public void setState(Map<String, ?> state);
}
