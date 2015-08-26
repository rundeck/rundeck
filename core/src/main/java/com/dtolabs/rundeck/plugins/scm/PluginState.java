package com.dtolabs.rundeck.plugins.scm;

import java.util.Map;

/**
 * Created by greg on 8/21/15.
 */
public interface PluginState {
    Map<String, ?> getState();
}
