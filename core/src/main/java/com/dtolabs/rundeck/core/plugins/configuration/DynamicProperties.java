package com.dtolabs.rundeck.core.plugins.configuration;

import org.rundeck.app.spi.Services;

import java.util.List;
import java.util.Map;

/**
 * Created by carlos on 04/01/18.
 */
public interface DynamicProperties {
    /**
     * Return dynamic allowed values for config properties, keyed by property name
     *
     * @param projectAndFrameworkValues config values for this plugin resolved from the framework/project
     */
    default Map<String, Object> dynamicProperties(Map<String, Object> projectAndFrameworkValues) {
        return null;
    }

    /**
     * Return dynamic allowed values for config properties, keyed by property name
     *
     * @param projectAndFrameworkValues config values for this plugin resolved from the framework/project
     * @param services                  authorized services access
     */
    default Map<String, Object> dynamicProperties(
        Map<String, Object> projectAndFrameworkValues,
        Services services
    )
    {
        return dynamicProperties(projectAndFrameworkValues);
    }
    /**
     * Return dynamic allowed values for config properties, keyed by property name
     *
     * @param projectAndFrameworkValues config values for this plugin resolved from the framework/project
     * @param services                  authorized services access
     */
    default Map<String, Object> dynamicDefaults(
        Map<String, Object> projectAndFrameworkValues,
        Services services
    )
    {
        return null;
    }

}
