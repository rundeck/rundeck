package com.dtolabs.rundeck.plugin

import com.dtolabs.rundeck.core.Constants
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.core.storage.StorageUtil
import com.dtolabs.rundeck.core.storage.files.FileStorageTree
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption
import com.dtolabs.rundeck.plugins.descriptions.RenderingOptions
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import com.dtolabs.rundeck.plugins.step.StepPlugin
import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.storage.api.Path
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.data.DataUtil

@Plugin(service = ServiceNameConstants.WorkflowStep, name =SaveFilePlugin. TYPE)
@PluginDescription(title = "Save File",description = "Save a file on storage")
class SaveFilePlugin  implements StepPlugin {
    public static final String TYPE = "savefile"

    @PluginProperty(title = "Path to file",
            description = "Storage path to file",
            required = true)
    String path

    @PluginProperty(title = "file name",
            description = "A name to file",
            required = true)
    String name

    @PluginProperty(title = "Content type",
            description = "File content type. Default: ",
            defaultValue = "text/plain",
            required = true)
    String contentType

    @PluginProperty(title = 'Overwrite if already exists', description = "Overwrite file if already exists")
    boolean overwrite = false

    @PluginProperty(title = 'Log content', description = "Log file content")
    boolean logContent = false

    @PluginProperty(title = "File content",
            description = "File content")
    @RenderingOptions(
            [
                    @RenderingOption(key = StringRenderingConstants.DISPLAY_TYPE_KEY, value = "MULTI_LINE"),
                    @RenderingOption(key = StringRenderingConstants.CODE_SYNTAX_MODE, value = "text/x-markdown")
            ]
    )
    String content

    @Override
    void executeStep(PluginStepContext context, Map<String, Object> configuration) throws NodeStepException {
        Path filePath = PathUtil.asPath(path + "/" + name)
        def dataContext = context.dataContextObject
        FileStorageTree fileStorageTree = context.executionContext.getFileStorageTree()
        byte[] fileContent = fileContent()
        boolean hasFile = fileStorageTree.hasFileOnExecWorkpacePath(filePath, dataContext.job.project, dataContext.job.name, dataContext.job.execid)

        if(!overwrite && hasFile){
            context.getLogger().log(Constants.ERR_LEVEL, "File already exists")
            throw new StepException(
                    "File already exists",
                    StepFailureReason.IOFailure
            )
        }

        Map<String, String> map = [:]
        if(contentType){
            map[StorageUtil.RES_META_RUNDECK_CONTENT_TYPE] = contentType
        }

        InputStream stream = new ByteArrayInputStream(fileContent)

        if(hasFile && overwrite){
            fileStorageTree.updateResource(
                    filePath,
                    DataUtil.withStream(stream, map, StorageUtil.factory()),
                    dataContext.job.project, dataContext.job.name, dataContext.job.execid
            )

            this.writeLog(context)

            return
        }

        fileStorageTree.createResource(
                filePath,
                DataUtil.withStream(stream, map, StorageUtil.factory()),
                dataContext.job.project, dataContext.job.name, dataContext.job.execid
        )

        this.writeLog(context)
    }

    private byte[] fileContent(){
        return content.getBytes()
    }

    private void writeLog(PluginStepContext context){
        if(logContent){
            Map<String, String> logPath = [:]
            logPath.put("Path", this.path)
            logPath.put("File name", this.name)
            logPath.put("Text", this.content)
            ObjectMapper objectMapper = new ObjectMapper()
            StringWriter stringWriter = new StringWriter()
            objectMapper.writeValue(stringWriter, logPath)


            context.getExecutionContext().getExecutionListener().log(
                    2,
                    stringWriter.toString(),
                    [
                            'content-data-type'       : 'application/json',
                            'content-meta:table-title': 'File saved on storage'
                    ]
            )
        }
    }
}
