package rundeck.services

import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.plugins.file.FileUploadPlugin
import grails.events.Listener
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.services.events.ExecutionBeforeStartEvent
import rundeck.services.events.ExecutionCompleteEvent

import java.nio.file.Files

/**
 * Manage receiving and retrieving files uploaded for job execution
 */
class FileUploadService {
    static transactional = false
    public static final String FS_FILE_UPLOAD_PLUGIN = 'filesystem-file-upload'
    PluginService pluginService
    ConfigurationService configurationService

    FileUploadPlugin getPlugin() {
        String pluginType = configurationService.getString('fileupload.plugin.type', FS_FILE_UPLOAD_PLUGIN)

        def plugin = pluginService.getPlugin(pluginType, FileUploadPlugin)
        plugin.initialize([:])
        plugin
    }

    /**
     * Upload a file for a particular input for a job
     * @param input
     * @param length
     * @param inputName
     * @param jobId
     * @return
     */
    String receiveFile(InputStream input, long length, String inputName, String jobId) {
        getPlugin().uploadFile(input, length, inputName, jobId)
    }

    def retrieveFile(OutputStream output, String reference) {
        getPlugin().retrieveFile(reference, output)
    }

    def removeFile(String reference) {
        getPlugin().removeFile(reference)
        removeReference(reference)
    }

    /**
     * Retrieve the file by reference, to a local temp file.  If the
     * file is already on local disk, it will be returned directly,
     * otherwise it will be retrieved from the plugin
     * @param reference
     * @return
     */
    File attachFileForExecution(String reference, Execution execution) {
        //TODO: domain object
        def file = retrieveTempFileForExecution(reference)
        registerExecutionFile(execution, reference)
        file
    }

    private void registerExecutionFile(Execution execution, String reference) {
        if (!executionFiles[execution.id]) {
            executionFiles[execution.id] = [reference]
        } else {
            executionFiles[execution.id] << reference
        }
    }
    Map<Long, List<String>> executionFiles = [:]
    /**
     * Retrieve the file by reference, to a local temp file.  If the
     * file is already on local disk, it will be returned directly,
     * otherwise it will be retrieved from the plugin
     * @param reference
     * @return
     */
    File retrieveTempFileForExecution(String reference) {
        def plugin = getPlugin()
        File file = plugin.retrieveLocalFile(reference)
        if (file) {
            return file
        }
        //copy locally
        file = Files.createTempFile(reference, "tmp").toFile()
        file.withOutputStream {
            plugin.retrieveFile(reference, it)
        }
        file.deleteOnExit()
        saveLocalReference(file, reference)
        file
    }

    /**
     * Return a map of option name to local file path, for file option types which have file references in the
     * input options map
     * @param scheduledExecution job
     * @param options input options mape
     * @return map of [optionName: filepath] for each loaded local file
     */
    Map<String, String> loadFileOptionInputs(
            Execution execution,
            ScheduledExecution scheduledExecution,
            StepExecutionContext context
    )
    {
        def loadedFiles = [:]
        def fileopts = scheduledExecution.listFileOptions()
        fileopts?.each {
            def key = context.dataContext['options'][it.name]
            if (key) {
                File file = attachFileForExecution(key, execution)
                loadedFiles[it.name] = file.absolutePath
                context.executionListener.log(3, "Retrieved file $key for option ${it.name} to $file.absolutePath")
            }
        }

        loadedFiles
    }

    /**
     * Before execution starts, load any file inputs for the job from storage
     * @param evt
     * @return
     */
    @Listener
    def executionBeforeStart(ExecutionBeforeStartEvent evt) {
        if (evt.job) {
            //handle uploaded files
            Map loadedFilePaths = loadFileOptionInputs(
                    evt.execution,
                    evt.job,
                    evt.context
            )
            if (loadedFilePaths) {
                evt.context.dataContext['file'] = loadedFilePaths
            }
        }
        evt.context
    }
    /**
     * Remove temp files
     * @param event
     */
    @Listener
    def executionComplete(ExecutionCompleteEvent e) {
        //TODO: don't necessarily auto-delete
        List<String> files = unregisterExecutionFiles(e.execution)
        log.error("remove files for execution $e.execution.id: $files")
        files?.each {
            removeFile(it)
        }
    }

    private List<String> unregisterExecutionFiles(Execution e) {
        def id = e.id
        def files = executionFiles.remove(id)
        files
    }

    //TODO:
    Map<String, File> localFileMap = [:]

    private void removeReference(String reference) {
        if (localFileMap[reference]) {
            localFileMap[reference].delete()
            localFileMap.remove(reference)
        }
    }

    private void saveLocalReference(File file, String reference) {
        localFileMap[reference] = file
    }
}
