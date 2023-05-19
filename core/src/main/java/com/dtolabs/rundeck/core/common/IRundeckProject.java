/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
     * @return creation time for configuration in epoch time
     */
    default Date getConfigCreatedTime(){
        return null;
    }

    /**
     * @return the project nodes interface
     */
    IProjectNodes getProjectNodes();

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

    /**
     * Indicates if the projects is enabled
     */
    default boolean isEnabled() { return true; }
}
