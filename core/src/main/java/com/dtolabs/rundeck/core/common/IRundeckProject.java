package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.authorization.Authorization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * Interface for a project
 */
public interface IRundeckProject extends IRundeckProjectConfig {
    /**
     * @return project name
     */
    public String getName();
    public IProjectInfo getInfo();
    /**
     * list the configurations of resource model providers.
     *
     * @return a list of maps containing:
     * <ul>
     * <li>type - provider type name</li>
     * <li>props - configuration properties</li>
     * </ul>
     */
    List<Map<String, Object>> listResourceModelConfigurations();

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
     * Update the resources file given an input Nodes set
     *
     * @param nodeset nodes
     *
     * @throws com.dtolabs.rundeck.core.common.UpdateUtils.UpdateException if an error occurs while trying to update the
     *                                                                     resources file or generate
     *                                                                     nodes
     */
    void updateNodesResourceFile(INodeSet nodeset) throws UpdateUtils.UpdateException;

    /**
     * @param name property name
     *
     * @return the property value by name
     */
    String getProperty(String name);

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
    Date getConfigLastModifiedTime();

    /**
     * @return the project nodes interface
     */
    IProjectNodes getProjectNodes();

    /**
     * @return authorization for this project
     */
    Authorization getProjectAuthorization();

    /**
     * @param path path relative to the project
     * @return true if it exists
     */
    boolean existsFileResource(String path);
    /**
     * @param path path relative to the project
     * @return true if it is a directory
     */
    boolean existsDirResource(String path);
    /**
     * @param path path relative to the project
     * @return list of paths within the directory
     */
    List<String> listDirPaths(String path);

    /**
     * @param path path relative to the project
     * @return true if it is deleted, false if it was not deleted
     */
    boolean deleteFileResource(String path);

    /**
     * Store a file at a path for the project
     * @param path path relative to the project
     * @param input input
     * @throws IOException if an IO error occurs
     */
    long storeFileResource(String path, InputStream input) throws IOException;

    /**
     * Read a file at a path for the project
     * @param path path relative to the project
     * @param output output
     * @return length of data loaded
     * @throws IOException if an IO error occurs
     */
    long loadFileResource(String path, OutputStream output) throws IOException;
}
