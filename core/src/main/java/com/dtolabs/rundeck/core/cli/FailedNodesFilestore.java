/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
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

package com.dtolabs.rundeck.core.cli;

import com.dtolabs.rundeck.core.cli.project.ProjectToolException;
import com.dtolabs.rundeck.core.execution.FailedNodesListener;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;
import com.dtolabs.rundeck.core.utils.NodeSet;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Utility class for managing the failed nodes filestore
 */
public class FailedNodesFilestore {
    /**
     * Mark the failedNodesFile for deletion if it exists.
     *
     * @param failedNodesFile file
     */
    public static void clearFailedNodesFile(final File failedNodesFile) {
        if (null != failedNodesFile && failedNodesFile.exists()) {
            failedNodesFile.deleteOnExit();
        }
    }

    /**
     * Store the list of failed nodes at the given file, or if it is empty remove the file
     *
     * @param nodenames       list of node names
     * @param failedNodesFile file for storing the failed nodes
     * @return true if storage completed, false otherwise
     */
    public static boolean storeFailedNodes(final Collection<String> nodenames, final File failedNodesFile) {
        if (null == nodenames || 0 == nodenames.size()) {
            clearFailedNodesFile(failedNodesFile);
        } else {
            //store properties
            final Properties props = new Properties();
            final StringBuffer sb = new StringBuffer();
            for (final String node : nodenames) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(node);
            }
            props.setProperty("failed.node.names", sb.toString());
            try {
                final FileOutputStream fileOutputStream = new FileOutputStream(failedNodesFile);
                try {
                    props.store(fileOutputStream, "Stored by " + ExecTool.class.getName());
                } finally {
                    fileOutputStream.close();
                }
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Create a FailedNodesListener implementation for the file.
     * @param failedNodesFile file
     * @return listener
     */
    public static FailedNodesListener createListener(final File failedNodesFile) {
        return new Listener(failedNodesFile);
    }

    static class Listener implements FailedNodesListener {
        static Logger logger = Logger.getLogger(FailedNodesFilestore.class);
        File failedNodesFile;

        public Listener(final File failedNodesFile) {
            this.failedNodesFile = failedNodesFile;
        }

        public void matchedNodes(Collection<String> names) {
            //noop
        }
        public void nodesSucceeded() {
            if (null != failedNodesFile) {
                //no nodes failed, so clear the file if it exists
                FailedNodesFilestore.clearFailedNodesFile(failedNodesFile);
                logger.debug("Failed node list file removed: " + failedNodesFile.getAbsolutePath());
            }
        }

        public void nodesFailed(final Map<String, NodeStepResult> failedNodeNames) {
            if (null != failedNodesFile) {
                if (failedNodeNames.size() > 0) {
                    //store failed node list into file, echo Commandline with nodelist
                    if (storeFailedNodes(failedNodeNames.keySet(), failedNodesFile)) {
                        logger.info("Stored failed node list in: " + failedNodesFile.getAbsolutePath());
                    } else {
                        logger.error("Unable to store failed node list in file: " + failedNodesFile.getAbsolutePath());
                    }

                }
            }
        }
    }

    /**
     * Parse the properties file specified and extract the failed node names, returning a filter map.
     *
     *@param failedNodesFile file
     * @return map of include filter key -&gt; nodename list.  Returns empty map if no results.
     */
    public static Map<String, String> parseFailedNodes(final File failedNodesFile) {
        Properties failedprops = new Properties();
        final HashMap<String, String> includeMap = new HashMap<String, String>();
        try {
            final FileInputStream fileInputStream = new FileInputStream(failedNodesFile);
            try {
                failedprops.load(fileInputStream);
            } finally {
                fileInputStream.close();
            }
            final String propval = failedprops.getProperty("failed.node.names");
            if (null != propval && !"".equals(propval.trim())) {
                final String failednodes = propval;
                includeMap.put(NodeSet.NAME, failednodes);
            }
        } catch (FileNotFoundException e) {
            throw new ProjectToolException(e);
        } catch (IOException e) {
            throw new ProjectToolException(e);
        }
        return includeMap;
    }

}