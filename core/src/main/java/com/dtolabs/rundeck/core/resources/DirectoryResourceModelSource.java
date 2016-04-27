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
* DirectoryResourceModelSource.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/21/11 11:13 AM
* 
*/
package com.dtolabs.rundeck.core.resources;

import com.dtolabs.rundeck.core.common.AdditiveListNodeSet;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.plugins.configuration.*;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParserService;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import com.dtolabs.rundeck.plugins.util.PropertyBuilder;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

/**
 * DirectoryResourceModelSource scans a directory for xml and yaml files, and loads all discovered files as nodes files
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class DirectoryResourceModelSource implements ResourceModelSource, ResourceModelSourceErrors, Configurable {
    static final Logger logger = Logger.getLogger(DirectoryResourceModelSource.class.getName());
    private final Framework framework;

    public DirectoryResourceModelSource(final Framework framework) {
        this.framework = framework;
    }

    private Configuration configuration;

    private final Map<File, ResourceModelSource> sourceCache =
            Collections.synchronizedMap(new HashMap<File, ResourceModelSource>());

    public static final Description DESCRIPTION = DescriptionBuilder.builder()
        .name("directory")
        .title("Directory")
        .description("Scans a directory and loads all resource document files")
        .property(PropertyBuilder.builder()
                      .string(Configuration.DIRECTORY)
                      .title("Directory Path")
                      .description("Directory path to scan")
                      .required(true)
                      .build()
        )
        .build();

    public void configure(final Properties configuration) throws ConfigurationException {

        final Configuration configuration1 = Configuration.fromProperties(configuration);
        configure(configuration1);
    }

    void configure(final Configuration config) throws ConfigurationException {
        this.configuration = config;
        configuration.validate();
    }

    public static class Configuration {
        public static final String DIRECTORY = "directory";
        public static final String PROJECT = "project";
        String project;
        File directory;

        public Configuration(final Properties config) {
            configure(config);
        }

        public static Configuration fromProperties(final Properties props) {
            return new Configuration(props);
        }

        public void configure(final Properties props) {
            if (props.containsKey(PROJECT)) {
                this.project = props.getProperty(PROJECT);
            }
            if (props.containsKey(DIRECTORY)) {
                this.directory = new File(props.getProperty(DIRECTORY));
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

    public INodeSet getNodes() throws ResourceModelSourceException {
        loadFileSources(configuration.directory, configuration.project);
        return loadNodeSets();
    }

    private List<String> sourceErrors;

    @Override
    public List<String> getModelSourceErrors() {
        return sourceErrors!=null?Collections.unmodifiableList(sourceErrors):null;
    }

    private INodeSet loadNodeSets() throws ResourceModelSourceException {
        synchronized (sourceCache) {
            ArrayList<String> errs = new ArrayList<>();
            AdditiveListNodeSet listNodeSet = new AdditiveListNodeSet();
            for (final File file : sortFiles(sourceCache.keySet())) {
                try {
                    listNodeSet.addNodeSet(sourceCache.get(file).getNodes());
                } catch(ResourceModelSourceException t){
                    String msg = "Error loading file: " +
                               file +
                               ": " +
                               t.getLocalizedMessage();
                    errs.add(msg);
                    logger.warn(msg);
                    logger.debug(msg,t);
                } catch(Throwable t){
                    String msg = "Error loading file: " +
                               file +
                               ": " +
                               t.getClass().getName() +
                               ": " +
                               t.getLocalizedMessage();
                    errs.add(msg);
                    logger.warn(msg);
                    logger.debug(msg,t);
                }
            }
            sourceErrors=errs;
            return listNodeSet;
        }
    }
    private File[] sortFiles(Collection<File> files){
        //sort on filename
        File[] arr = files.toArray(new File[files.size()]);
        Arrays.sort(
                arr, new Comparator<File>() {
                    public int compare(final File file, final File file1) {
                        return file.getName().compareTo(file1.getName());
                    }
                }
        );
        return arr;
    }

    /**
     * Discover new files in the directory, and add file sources
     */
    private void loadFileSources(final File directory, final String project) {
        //clear source sequence

        if (!directory.isDirectory()) {
            logger.warn("Not a directory: " + directory);
        }
        //get supported parser extensions
        final Set<String> exts = new HashSet<String>(
            framework.getResourceFormatParserService().listSupportedFileExtensions());
        final File[] files = directory.listFiles(new FilenameFilter() {
            public boolean accept(final File file, final String s) {
                return exts.contains(
                    ResourceFormatParserService.getFileExtension(s));
            }
        });

        //set of previously cached file sources by file
        if (null != files) {
            //sort on filename
            Arrays.sort(files, null);
            synchronized (sourceCache) {
                final HashSet<File> trackedFiles = new HashSet<File>(sourceCache.keySet());
                for (final File file : files) {
                    //remove file that we want to keep
                    trackedFiles.remove(file);
                    if (!sourceCache.containsKey(file)) {
                        logger.debug("Adding new resources file to cache: " + file.getAbsolutePath());
                        try {
                            final ResourceModelSource source = createFileSource(project, file);
                            sourceCache.put(file, source);

                        } catch (ExecutionServiceException e) {
                            e.printStackTrace();
                            logger.debug("Failed adding file " + file.getAbsolutePath() + ": " + e.getMessage(), e);
                        }
                    }
                }
                //remaining trackedFiles are files that have been removed from the dir
                for (final File oldFile : trackedFiles) {
                    logger.debug("Removing from cache: " + oldFile.getAbsolutePath());
                    sourceCache.remove(oldFile);
                }
            }
        }
    }

    private ResourceModelSource createFileSource(String project, File file) throws ExecutionServiceException {
        Properties properties1 = FileResourceModelSource.Configuration.build()
                                                                      .project(project)
                                                                      .file(file)
                                                                      .generateFileAutomatically(
                                                                              false
                                                                      )
                                                                      .includeServerNode(
                                                                              false
                                                                      )
                                                                      .getProperties();
        return framework.getResourceModelSourceService().getSourceForConfiguration(
                "file",
                properties1
        );
    }

    @Override
    public String toString() {
        return "DirectoryResourceModelSource{" +
               "directory=" + configuration.directory +
               '}';
    }
}
