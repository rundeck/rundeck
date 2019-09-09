package com.dtolabs.rundeck.plugin

import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.storage.StorageUtil
import com.dtolabs.rundeck.core.storage.files.FileStorageTree
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import com.dtolabs.rundeck.plugins.step.StepPlugin

@Plugin(service = ServiceNameConstants.WorkflowStep, name =DeleteFilePlugin.TYPE)
@PluginDescription(title = "Delete File",description = "Delete files from storage")
class DeleteFilePlugin implements StepPlugin {
    public static final String TYPE = "deletefile"

    @PluginProperty(title = "Job name to delete files",
            description = "Only required if bulk delete is 'Job'",
            required = true)
    String jobName


    @Override
    void executeStep(PluginStepContext context, Map<String, Object> configuration) throws NodeStepException {
        FileStorageTree fileStorageTree = context.executionContext.getFileStorageTree()
        def dataContext = context.dataContextObject
        String project = dataContext.job.project
        String jobName = DataContextUtils.replaceDataReferencesInString(this.jobName, dataContext)

        StorageUtil.deletePathRecursive(fileStorageTree, fileStorageTree.getJobFilesPath(project, jobName))

        this.writeLog(context)
    }

    private void writeLog(PluginStepContext context){
        context.getExecutionContext().getExecutionListener().log(
                2,
                "Files removed"
        )
    }
}
