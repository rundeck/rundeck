package com.dtolabs.rundeck.core.common;

import java.util.Properties;
import java.util.Set;

/**
 * Created by greg on 2/2/16.
 */
public interface IRundeckProjectConfigModifier {

    /**
     * Update the project properties file by setting updating the given properties, and removing
     * any properties that have a prefix in the removePrefixes set
     *
     * @param properties     new properties to put in the file
     * @param removePrefixes prefixes of properties to remove from the file
     */
    void mergeProjectProperties(Properties properties, Set<String> removePrefixes);

    /**
     * Set the project properties file contents exactly
     *
     * @param properties new properties to use in the file
     */
    void setProjectProperties(Properties properties);

    void generateProjectPropertiesFile(boolean overwrite, Properties properties, boolean addDefault);
}
