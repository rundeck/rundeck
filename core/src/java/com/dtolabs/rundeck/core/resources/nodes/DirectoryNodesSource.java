/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* DirectoryNodesSource.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/21/11 11:13 AM
* 
*/
package com.dtolabs.rundeck.core.resources.nodes;

import com.dtolabs.rundeck.core.common.AdditiveListNodeSet;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

/**
 * DirectoryNodesSource scans a directory for xml and yaml files, and loads all discovered files as nodes files
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class DirectoryNodesSource implements NodesSource, Configurable {
    static final Logger logger = Logger.getLogger(DirectoryNodesSource.class.getName());
    private final Framework framework;

    public DirectoryNodesSource(final Framework framework) {
        this.framework = framework;
    }

    private Configuration configuration;
    long lastModTime = 0;
    private AdditiveListNodeSet listNodeSet = new AdditiveListNodeSet();
    private ArrayList<NodesSource> fileSources = new ArrayList<NodesSource>();
    private HashMap<File, NodesSource> sourceCache = new HashMap<File, NodesSource>();

    public void configure(final Properties configuration) throws ConfigurationException {

        final Configuration configuration1 = Configuration.fromProperties(configuration);
        configure(configuration1);
    }

    void configure(final Configuration config) throws ConfigurationException {
        this.configuration = config;
        configuration.validate();
    }

    public static class Configuration {
        String project;
        File directory;

        public Configuration(final Properties config) {
            configure(config);
        }

        public static Configuration fromProperties(final Properties props) {
            return new Configuration(props);
        }

        public void configure(final Properties props) {
            if (props.containsKey("project")) {
                this.project = props.getProperty("project");
            }
            if (props.containsKey("directory")) {
                this.directory = new File(props.getProperty("directory"));
            }
        }

        public void validate() throws ConfigurationException {
            if (null == project) {
                throw new ConfigurationException("project is required");
            }
            if (null == directory) {
                throw new ConfigurationException("directory is required");
            }
            if (directory.isFile()) {
                throw new ConfigurationException("path specified is not a directory: " + directory);
            }
        }

    }

    public INodeSet getNodes() throws NodesSourceException {
        loadFileSources(configuration.directory, configuration.project);
        listNodeSet = new AdditiveListNodeSet();
        loadNodeSets();
        return listNodeSet;
    }

    private void loadNodeSets() throws NodesSourceException {
        for (final NodesSource fileSource: fileSources) {
            try {
                listNodeSet.addNodeSet(fileSource.getNodes());
            } catch (NodesSourceException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Discover new files in the directory, and add file sources
     */
    private void loadFileSources(final File directory, final String project) {
        //clear source sequence
        fileSources.clear();
        if (!directory.isDirectory()) {
            logger.warn("Not a directory: " + directory);
        }
        final File[] files = directory.listFiles(new FilenameFilter() {
            public boolean accept(final File file, final String s) {
                return s.endsWith(".xml") || s.endsWith(".yaml");
            }
        });

        //set of previously cached file sources by file
        final HashSet<File> trackedFiles = new HashSet<File>(sourceCache.keySet());
        if (null != files) {
            //sort on filename
            Arrays.sort(files, new Comparator<File>() {
                public int compare(final File file, final File file1) {
                    return file.getName().compareTo(file1.getName());
                }
            });
            for (final File file : files) {
                //remove file that we want to keep
                trackedFiles.remove(file);
                if (!sourceCache.containsKey(file)) {
                    try {
                        final NodesSource source = framework.getNodesSourceService().getSourceForConfiguration(
                            "file",
                            FileNodesSource.Configuration.build()
                                .project(project)
                                .file(file)
                                .generateFileAutomatically(false)
                                .includeServerNode(false).getProperties()
                        );
                        fileSources.add(source);
                        sourceCache.put(file, source);

                    } catch (ExecutionServiceException e) {
                        e.printStackTrace();
                    }
                } else {
                    fileSources.add(sourceCache.get(file));
                }
            }
        }
        //remaining trackedFiles are files that have been removed from the dir
        for (final File oldFile : trackedFiles) {
            sourceCache.remove(oldFile);
        }
    }
}
