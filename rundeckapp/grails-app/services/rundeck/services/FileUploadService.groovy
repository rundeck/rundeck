package rundeck.services

import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.plugins.file.FileUploadPlugin
import grails.events.Listener
import grails.transaction.Transactional
import rundeck.Execution
import rundeck.JobFileRecord
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
    public static final String RECORD_TYPE_OPTION_INPUT = 'option'
    PluginService pluginService
    ConfigurationService configurationService

    FileUploadPlugin getPlugin() {
        String pluginType = getPluginType()

        def plugin = pluginService.getPlugin(pluginType, FileUploadPlugin)
        plugin.initialize([:])
        plugin
    }

    private String getPluginType() {
        configurationService.getString('fileupload.plugin.type', FS_FILE_UPLOAD_PLUGIN)
    }

    /**
     * Upload a file for a particular input for a job
     * @param input
     * @param length
     * @param inputName
     * @param jobId
     * @return
     */
    @Transactional
    String receiveFile(InputStream input, long length, String username, String inputName, String jobId) {
        UUID uuid = UUID.randomUUID()
        def refid = getPlugin().uploadFile(input, length, uuid.toString())
        log.error("uploadedFile $uuid refid $refid")
        def record = createRecord(refid, length, uuid, jobId, username)
        log.error("record: $record")
        refid
    }

    JobFileRecord createRecord(String refid, long length, UUID uuid, String jobId, String username) {
        def jfr = new JobFileRecord(
                fileName: refid,
                size: length,
                recordType: RECORD_TYPE_OPTION_INPUT,
                expirationDate: null,//(new Date() + 1l),
                retained: false,
                available: true,
                uuid: uuid.toString(),
                jobId: jobId,
                storageType: getPluginType(),
                user: username,
                storageReference: refid,
                storageMeta: null//metadata
        )
        if(!jfr.validate()){
            throw new RuntimeException("Could not validate record: $jfr.errors")
        }
        jfr.save(flush: true)
    }

    def retrieveFile(OutputStream output, String reference) {
        getPlugin().retrieveFile(reference, output)
    }

    def removeFile(JobFileRecord record) {
        getPlugin().removeFile(record.storageReference)
        removeRecord(record)
    }

    /**
     * Retrieve the file by reference, to a local temp file.  If the
     * file is already on local disk, it will be returned directly,
     * otherwise it will be retrieved from the plugin
     * @param reference
     * @return
     */
    File attachFileForExecution(String reference, Execution execution) {
        JobFileRecord jfr = findRecord(reference)
        def file = retrieveTempFileForExecution(reference)
        jfr.execution = execution
        jfr.available = true
        jfr.save(flush: true)
        file
    }

    List<JobFileRecord> findRecords(final Execution execution) {
        JobFileRecord.findAllByExecution(execution)
    }

    List<JobFileRecord> findRecords(final Execution execution, String recordType) {
        JobFileRecord.findAllByExecutionAndRecordType(execution, recordType)
    }

    List<JobFileRecord> findRecords(final String jobid) {
        JobFileRecord.findAllByJobId(jobid)
    }

    List<JobFileRecord> findRecords(final String jobid, String recordType) {
        JobFileRecord.findAllByJobIdAndRecordType(jobid, recordType)
    }

    JobFileRecord findRecord(final String reference) {
        JobFileRecord.findByStorageReference(reference)
    }

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
            def key = context.dataContext['option'][it.name]
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
        findRecords(e.execution, RECORD_TYPE_OPTION_INPUT)?.each {
            removeFile(it)
        }
    }

    private List<JobFileRecord> unregisterExecutionFiles(Execution e) {
        findRecords(e, RECORD_TYPE_OPTION_INPUT)
    }

    //TODO:
    Map<String, File> localFileMap = [:]

    private void removeRecord(JobFileRecord record) {
        def reference = record.storageReference
        if (localFileMap[reference]) {
            localFileMap[reference].delete()
            localFileMap.remove(reference)
        }
        record.available = false
        record.save(flush: true)
    }

    private void saveLocalReference(File file, String reference) {
        localFileMap[reference] = file
    }
}
