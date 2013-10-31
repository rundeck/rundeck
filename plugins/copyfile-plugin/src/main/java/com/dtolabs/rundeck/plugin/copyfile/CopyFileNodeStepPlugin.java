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

import java.io.File;
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
    @PluginProperty(title = "Print transfer information", description = "Log information about the file copy", defaultValue = "true")
    private boolean echo;

    public static enum Reason implements FailureReason {
        CopyFileFailed,

    }

    @Override
    public void executeNodeStep(PluginStepContext context, Map<String, Object> configuration,
            INodeEntry entry) throws NodeStepException {
        File file = new File(sourcePath);
        String customDestinationPath = destinationPath;
        if (destinationPath.endsWith("/")) {
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
}
