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

    @PluginProperty(title = "Source Path", description = "Path on the rundeck server for the file.", required = true)
    private String sourcePath;
    @PluginProperty(title = "Destination Path", description = "Path on the remote node for the file destination. If " +
            "the path ends with a /, the same filename as the source will be used.", required = true)
    private String destinationPath;
    @PluginProperty(title = "Recursive copy", description = "If this is set the plugin is going to copy a folder with his content on the destintion path.", defaultValue = "false")
    private boolean recursive;
    @PluginProperty(title = "Allow wildcard", description = "If this is set the plugin is going to search any file that match the wildcard(*) and copy it in the destination Path.", defaultValue = "false")
    private boolean wildcards;
    @PluginProperty(title = "Print transfer information", description = "Log information about the file copy", defaultValue = "true")
    private boolean echo;

    public static enum Reason implements FailureReason {
        CopyFileFailed,
        WrongParameter
    }

    private List<File> fileList;

    private String separator = "/";

    @Override
    public void executeNodeStep(PluginStepContext context, Map<String, Object> configuration,
                                INodeEntry entry) throws NodeStepException {
        separator = System.getProperty("file.separator");
        fileList = new ArrayList<>();

        if(recursive && !wildcards){
            File folder = new File(sourcePath);
            if(!folder.isDirectory()){
                throw new NodeStepException("sourcePath has to be a directory", Reason.WrongParameter, entry.getNodename());
            }
            if (!destinationPath.endsWith("/")) {
                destinationPath = destinationPath + "/";
            }
            copyFile(sourcePath,destinationPath,context, entry);
        }else if(wildcards) {
            String basefolder = "";
            String search = "";
            String family = ("/".equals(separator) ? "unix" : "\\".equals(separator) ? "windows" : "");
            // determine base folder for search
            if(family.equals("unix")){
                basefolder = "/";
                search = sourcePath.substring(1);
            }else{
                int index=sourcePath.indexOf(":\\");
                basefolder=sourcePath.substring(0,index+2);
                search = sourcePath.substring(index+2);
            }
            if(echo) {
                context.getLogger().log(2, "Searching : '" + search + "' on:'"+basefolder+"'");
            }
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir(basefolder);
            scanner.setIncludes(new String[]{search});
            scanner.setFollowSymlinks(true);
            scanner.scan();
            String[] files = scanner.getIncludedFiles();
            String customDestinationPath = destinationPath;
            if (!destinationPath.endsWith("/")) {
                //always copy to a folder
                destinationPath = customDestinationPath + "/";
            }
            for(String file : files){
                String toCopyFile = basefolder+file;
                if(echo) {
                    context.getLogger().log(2, "To copy file: " + toCopyFile);
                }
                File source = new File(toCopyFile);

                fileList.add(source);
            }
            if(recursive){
                String[] folders = scanner.getIncludedDirectories();
                for(String folder : folders){
                    String toCopyFolder = basefolder+folder;
                    if(echo) {
                        context.getLogger().log(2, "To copy complete folder: " + toCopyFolder);
                    }
                    File source = new File(toCopyFolder);

                    fileList.add(source);
                }
            }
            copyFileList(context, entry);

        }else{
            copyFile(sourcePath, destinationPath, context, entry);
        }

    }


    private void copyFile(String sourcePath, String destinationPath, PluginStepContext context, INodeEntry entry )
            throws NodeStepException{
        File file = new File(sourcePath);
        String customDestinationPath = destinationPath;
        if (destinationPath.endsWith("/") && !file.isDirectory()) {
            customDestinationPath = customDestinationPath + file.getName();
        }
        try {
            if(echo) {
                context.getLogger().log(2, "Begin copy " + file.length() + " bytes to node " + entry
                        .getNodename() + ": " + file.getAbsolutePath() + " -> " +
                        customDestinationPath);
            }
            String path = context.getFramework().getExecutionService().fileCopyFile(context.getExecutionContext(), file, entry,
                    customDestinationPath);
            if (echo) {
                context.getLogger().log(2, "Copied: " + path);
            }
        } catch (FileCopierException e) {
            context.getLogger().log(0, "failed: " + e.getMessage());
            throw new NodeStepException(e, Reason.CopyFileFailed, entry.getNodename());
        }
    }

    private void copyFileList(PluginStepContext context, INodeEntry entry )
            throws NodeStepException{
        if(fileList.size() == 0){
            if(echo) {
                context.getLogger().log(2, "No matching files" );
            }
        }else {
            try {
                if (echo) {
                    context.getLogger().log(2, "Begin copy " + fileList.size() + " files  to node " + entry
                            .getNodename());
                }
                String[] paths = context.getFramework().getExecutionService().fileCopyFiles(context.getExecutionContext(), fileList, destinationPath, entry);
                if (echo) {
                    for (String path : paths) {
                        context.getLogger().log(2, "Copied: " + path);
                    }
                }
            } catch (FileCopierException e) {
                context.getLogger().log(0, "failed: " + e.getMessage());
                throw new NodeStepException(e, Reason.CopyFileFailed, entry.getNodename());
            }
        }
    }
}
