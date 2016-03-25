package com.dtolabs.rundeck.core.common;

import java.util.Date;
import java.util.Map;

/**
 * definition of a project's configuration
 */
public interface IRundeckProjectConfig {
    /**
     * @return project name
     */
    public String getName();
    /**
     * @param name property name
     *
     * @return the property value by name
     */
    String getProperty(String name);

    /**
     *
     * @param key property name
     * @return true if present, false otherwise
     */
    boolean hasProperty(String key);

    /**
     * @return the merged properties available for the project
     */
    Map<String,String> getProperties();

    /**
     * @return the direct properties set for the project
     */
    Map<String,String> getProjectProperties();
    /**
     * @return last modified time for configuration in epoch time
     */
    Date getConfigLastModifiedTime();
}
