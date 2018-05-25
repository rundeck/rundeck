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

package com.dtolabs.rundeck.plugin.copyfile;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.service.FileCopierException;
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import org.apache.tools.ant.DirectoryScanner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * $INTERFACE is ... User: greg Date: 10/31/13 Time: 2:58 PM
 */
@Plugin(service = ServiceNameConstants.WorkflowNodeStep, name = CopyFileNodeStepPlugin.TYPE)
@PluginDescription(title = "Copy File",description = "Copy a file to a destination on a remote node.")
public class CopyFileNodeStepPlugin implements NodeStepPlugin {
    public static final String TYPE = "copyfile";
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");

    @PluginProperty(title = "Source Path",
                    description = "Path on the rundeck server for the file or base directory (recursive/wildcard " +
                                  "search).",
                    required = true)
    String sourcePath;
    @PluginProperty(title = "Destination Path", description = "Path on the remote node for the file destination. If " +
            "the path ends with a /, the same filename as the source will be used.", required = true)
    String destinationPath;
    @PluginProperty(title = "Pattern", description = "Wildcard pattern (optional). E.g: `**/*.txt`", required = false)
    String pattern;
    @PluginProperty(title = "Recursive copy",
                    description = "Recursively copy source dir, or matched files and dirs to the destination path.",
                    defaultValue = "false")
    boolean recursive;
    @PluginProperty(title = "Print transfer information",
                    description = "Log information about the file copy",
                    defaultValue = "true")
    boolean echo;

    public static enum Reason implements FailureReason {
        CopyFileFailed,
        WrongParameter
    }



    @Override
    public void executeNodeStep(
            PluginStepContext context,
            Map<String, Object> configuration,
            INodeEntry entry
    )
            throws NodeStepException
    {

        String destinationPathDir = destinationPath.endsWith("/") ? destinationPath : destinationPath + "/";
        File folder = new File(sourcePath);
        if (recursive || pattern != null) {
            if (!folder.isDirectory()) {
                throw new NodeStepException(
                        "sourcePath has to be a directory",
                        Reason.WrongParameter,
                        entry.getNodename()
                );
            }
        }
        if (recursive && pattern == null) {
            copyFile(sourcePath, destinationPathDir, context, entry);
        } else if (pattern != null) {
            List<File> fileList = new ArrayList<>();
            File basedir = new File(sourcePath);
            if (echo) {
                context.getLogger().log(2, String.format("Searching : '%s' on: '%s'", pattern, sourcePath));
            }
            DirectoryScanner scanner = scanDirectory(context, fileList, basedir, sourcePath, pattern);
            if (recursive) {
                for (String file : scanner.getIncludedDirectories()) {
                    if ("".equals(file)) {
                        continue;
                    }
                    File source = new File(sourcePath, file);
                    if (echo) {
                        context.getLogger().log(2, String.format("To copy directory: %s", source.getAbsolutePath()));
                    }

                    fileList.add(source);
                }
            }
            copyFileList(folder, fileList, destinationPathDir, context, entry);
        } else {
            copyFile(sourcePath, destinationPath, context, entry);
        }

    }

    private DirectoryScanner scanDirectory(
            final PluginStepContext context,
            final List<File> fileList,
            final File basedir,
            final String sourcePath,
            final String pattern
    )
    {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(basedir);
        scanner.setIncludes(new String[]{pattern});
        scanner.setFollowSymlinks(true);
        scanner.scan();
        for (String file : scanner.getIncludedFiles()) {
            File source = new File(sourcePath, file);
            if (echo) {
                context.getLogger().log(2, String.format("To copy file: %s", source.getAbsolutePath()));
            }

            fileList.add(source);
        }
        return scanner;
    }


    private void copyFile(String sourcePath, String destinationPath, PluginStepContext context, INodeEntry entry)
            throws NodeStepException
    {
        File file = new File(sourcePath);
        String customDestinationPath = destinationPath;
        if (destinationPath.endsWith("/") && !file.isDirectory()) {
            customDestinationPath = customDestinationPath + file.getName();
        }
        try {
            if (echo) {
                context.getLogger().log(
                        2,
                        String.format(
                                "Begin copy %d bytes to node %s: %s -> %s",
                                file.length(),
                                entry.getNodename(),
                                file.getAbsolutePath(),
                                customDestinationPath
                        )
                );
            }
            String path = context.getFramework().getExecutionService().fileCopyFile(
                    context.getExecutionContext(),
                    file,
                    entry,
                    customDestinationPath
            );
            if (echo) {
                context.getLogger().log(2, "Copied: " + path);
            }
        } catch (FileCopierException e) {
            context.getLogger().log(0, "failed: " + e.getMessage());
            throw new NodeStepException(e, Reason.CopyFileFailed, entry.getNodename());
        }
    }

    private void copyFileList(
            File basedir,
            List<File> fileList,
            String destinationPath,
            PluginStepContext context,
            INodeEntry entry
    )
            throws NodeStepException{
        if(fileList.size() == 0){
            if(echo) {
                context.getLogger().log(2, "No matching files" );
            }
        }else {
            try {
                if (echo) {
                    context.getLogger().log(
                            2,
                            String.format(
                                    "Begin copy %d files to node %s",
                                    fileList.size(),
                                    entry.getNodename()
                            )
                    );
                }
                String[] paths = context.getFramework()
                                        .getExecutionService()
                                        .fileCopyFiles(
                                                context.getExecutionContext(),
                                                basedir,
                                                fileList,
                                                destinationPath,
                                                entry
                                        );
                if (echo) {
                    for (String path : paths) {
                        context.getLogger().log(2, String.format("Copied: %s", path));
                    }
                }
            } catch (FileCopierException e) {
                context.getLogger().log(0, String.format("failed: %s", e.getMessage()));
                throw new NodeStepException(e, Reason.CopyFileFailed, entry.getNodename());
            }
        }
    }
}
