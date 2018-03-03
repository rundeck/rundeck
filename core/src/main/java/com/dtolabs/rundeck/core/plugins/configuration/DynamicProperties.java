package com.dtolabs.rundeck.core.plugins.configuration;

import java.util.Map;

/**
 * Created by carlos on 04/01/18.
 */
public interface DynamicProperties {
    Map<String, Object> dynamicProperties (Map<String, Object> projectAndFrameworkValues);
}
