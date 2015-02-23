package com.dtolabs.rundeck.core.common;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Interface for a project
 */
public interface IRundeckProject {
    /**
     * @return project name
     */
    public String getName();
    /**
     * list the configurations of resource model providers.
     *
     * @return a list of maps containing:
     * <ul>
     * <li>type - provider type name</li>
     * <li>props - configuration properties</li>
     * </ul>
     */
    List<Map> listResourceModelConfigurations();

    /**
     * Returns the set of nodes for the project
     *
     * @return an instance of {@link INodeSet}
     *
     * @throws NodeFileParserException on parse error
     */
    INodeSet getNodeSet() throws NodeFileParserException;

    /**
     * Conditionally update the nodes resources file if a URL source is defined for it and return
     * true if the update process was invoked and succeeded
     *
     * @return true if the update succeeded, false if it was not performed
     * @throws com.dtolabs.rundeck.core.common.UpdateUtils.UpdateException if an error occurs while trying to update the resources file
     *
     */
    boolean updateNodesResourceFile() throws UpdateUtils.UpdateException;

    /**
     * Update the nodes resources file from a specific URL, with BASIC authentication as provided or
     * as defined in the URL's userInfo section.
     * @param providerURL URL to retrieve resources file definition
     * @param username username or null
     * @param password or null
     * @throws com.dtolabs.rundeck.core.common.UpdateUtils.UpdateException if an error occurs during the update process
     */
    void updateNodesResourceFileFromUrl(
            String providerURL, String username,
            String password
    ) throws UpdateUtils.UpdateException;

    /**
     * @param name property name
     *
     * @return the property value by name
     */
    String getProperty(String name);

    boolean hasProperty(String key);

    Map<String,?> getProperties();

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

    /**
     * @return last modified time for configuration in epoch time
     */
    long getConfigLastModifiedTime();
}
