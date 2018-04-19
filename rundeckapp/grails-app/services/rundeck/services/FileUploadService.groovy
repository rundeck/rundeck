package rundeck.services

import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.plugins.file.FileUploadPlugin
import grails.events.annotation.Subscriber
import grails.events.annotation.gorm.Listener
import grails.gorm.transactions.Transactional
import org.rundeck.util.SHAInputStream
import org.rundeck.util.SHAOutputStream
import org.rundeck.util.Sizes
import org.rundeck.util.ThresholdInputStream
import rundeck.Execution
import rundeck.JobFileRecord
import rundeck.Option
import rundeck.ScheduledExecution
import rundeck.services.events.ExecutionPrepareEvent
import rundeck.services.events.ExecutionCompleteEvent

import java.nio.file.Files

/**
 * Manage receiving and retrieving files uploaded for job execution
 */
class FileUploadService {
    static transactional = false
    public static final String FS_FILE_UPLOAD_PLUGIN = 'filesystem-temp'
    public static final String RECORD_TYPE_OPTION_INPUT = 'option'
    public static final long DEFAULT_TEMP_EXPIRATION = 10 * 60 * 1000 //10 minutes
    public static final String DEFAULT_MAX_SIZE = '200MB'
    PluginService pluginService
    ConfigurationService configurationService
    TaskService taskService
    FrameworkService frameworkService
    def executorService

    long getTempfileExpirationDelay() {
        configurationService.getLong "fileUploadService.tempfile.expiration", DEFAULT_TEMP_EXPIRATION
    }

    String getOptionUploadMaxSizeString() {
        String max = configurationService.getString "fileUploadService.tempfile.maxsize", DEFAULT_MAX_SIZE
        Long val = Sizes.parseFileSize(max)
        if (val == null) {
            log.warn(
                    "Invalid value for: rundeck.fileUploadService.tempfile.maxsize ($max), using default " +
                            "$DEFAULT_MAX_SIZE"
            )
            DEFAULT_MAX_SIZE
        }
        max
    }

    long getOptionUploadMaxSize() {
        Sizes.parseFileSize(optionUploadMaxSizeString)
    }

    def getPluginDescription(){
        pluginService.getPluginDescriptor(pluginType, FileUploadPlugin)?.description
    }

    def validatePluginConfig(Map configMap) {
        pluginService.validatePluginConfig(pluginType, FileUploadPlugin, configMap)
    }
    Validator.Report validateFileOptConfig(Option opt) {
        if (opt.typeFile) {
            def result = validatePluginConfig(opt.configMap)
            if (!result.valid) {

                opt.errors.rejectValue(
                        'configMap',
                        'option.file.config.invalid.message',
                        [result.report.toString()].toArray(),
                        "invalid file config: {0}"
                )
                return result.report
            }
        }
    }


    FileUploadPlugin getPlugin() {
        def configured = pluginService.configurePlugin(pluginType, [:], FileUploadPlugin)
        def plugin = configured.instance
        plugin.initialize()
        return plugin
    }

    private String getPluginType() {
        configurationService.getString('fileupload.plugin.type', FS_FILE_UPLOAD_PLUGIN)
    }

    def listPlugins() {
        pluginService.listPlugins(FileUploadPlugin)
    }

    /**
     * Upload a file for a particular input for a job
     * @param input
     * @param length
     * @param inputName
     * @param jobId
     * @return unique file id
     * @throws FileUploadServiceException if the uploaded file exceeds the maximum size
     * @throws IOException if there is an error copying the uploaded file
     *
     */
    @Transactional
    String receiveFile(
            InputStream input,
            long length,
            String username,
            String origName,
            String inputName,
            Map inputConfig,
            String jobId,
            String project,
            Date expiryStart
    )
    {

        def shastream = new SHAInputStream(input)
        def readstream = shastream
        long max = optionUploadMaxSize
        if (max > 0) {
            if (length > max) {
                throw new FileUploadServiceException(
                        "Uploaded file size ($length) is larger than configured maximum file size: " +
                                "$optionUploadMaxSizeString"
                )
            }
            readstream = new ThresholdInputStream(readstream, max)
        }
        UUID uuid = UUID.randomUUID()
        def fileref
        def uuidstring = uuid.toString()
        try {
            fileref = getPlugin().uploadFile(readstream, length, uuidstring, inputConfig)
        } catch (ThresholdInputStream.Threshold e) {
            throw new FileUploadServiceException(
                    "Uploaded file data size ($e.breach) is larger than configured maximum file size: " +
                            "$optionUploadMaxSizeString"
            )
        } catch (IOException e) {
            throw new FileUploadServiceException("Error receiving file: " + e.message, e)
        }
        def shaString = shastream.SHAString
        log.debug("uploadedFile $uuid refid $fileref (sha $shaString)")
        def record = createRecord(
                fileref,
                length,
                uuid,
                shaString,
                origName,
                jobId,
                inputName,
                username,
                project,
                expiryStart
        )
        log.debug("record: $record")
        if (expiryStart) {
            Long id = record.id
            taskService.runAt(record.expirationDate, "expire:$uuidstring") {
                JobFileRecord.withNewSession {
                    expireRecordIfNeeded(id)
                }
            }
        }
        uuidstring
    }

    /**
     * Find expired FileUploadRecords and remove them
     */
    @Transactional
    def checkAndExpireAllRecords() {
        findExpiredRecords(frameworkService.serverUUID)?.each(this.&expireRecord)
    }

    @Transactional
    def onBootstrap() {
        executorService.submit {
            checkAndExpireAllRecords()
        }
    }

    private void expireRecordIfNeeded(long id) {
        JobFileRecord record = JobFileRecord.get(id)
        Date now = new Date()
        if (record && record.expirationDate && now > record.expirationDate && record.stateIsTemp()) {
            expireRecord(record)
        }
    }

    private void expireRecord(JobFileRecord record) {
        try {
            def uuid = record.uuid
            changeFileState(record, FileUploadPlugin.ExternalState.Unused)
            log.debug("Job File Record Expired: $uuid")
        } catch (Throwable t) {
            log.error("Failed removing expired file with plugin: " + t, t)
        }
    }

    JobFileRecord createRecord(
            String refid,
            long length,
            UUID uuid,
            String shaString,
            String origName,
            String jobId,
            String inputName,
            String username,
            String project,
            Date expiryStart
    )
    {
        def expirationDate = expiryStart ? (new Date(expiryStart.time + tempfileExpirationDelay)) : null
        def jfr = new JobFileRecord(
                fileName: origName,
                size: length,
                recordType: RECORD_TYPE_OPTION_INPUT,
                expirationDate: expirationDate,
                fileState: JobFileRecord.STATE_TEMP,
                uuid: uuid.toString(),
                serverNodeUUID: frameworkService.serverUUID,
                sha: shaString,
                jobId: jobId,
                recordName: inputName,
                storageType: getPluginType(),
                user: username,
                storageReference: refid,
                project: project,
        )
        if(!jfr.validate()){
            throw new RuntimeException("Could not validate record: $jfr.errors")
        }
        jfr.save(flush: true)
    }

    private def changeFileState(JobFileRecord record, FileUploadPlugin.ExternalState extState) {
        def result = getPlugin().transitionState(record.storageReference, extState)
        String toState = FileUploadPlugin.InternalState.Deleted == result ?
                (FileUploadPlugin.ExternalState.Unused == extState ?
                        JobFileRecord.STATE_EXPIRED :
                        JobFileRecord.STATE_DELETED
                ) :
                JobFileRecord.STATE_RETAINED

        record.state(toState)
        record.save(flush: true)
        if (result == FileUploadPlugin.InternalState.Deleted) {
            removeLocalFile(record.storageReference)
        }
    }


    def deleteRecord(JobFileRecord record) {
        def plugin = getPlugin()
        def reference = record.storageReference
        if (plugin.hasFile(reference)) {
            plugin.transitionState(reference, FileUploadPlugin.ExternalState.Deleted)
        }
        removeLocalFile(reference)
        record.delete()
    }

    def deleteRecordsForExecution(Execution e) {
        JobFileRecord.findAllByExecution(e).each this.&deleteRecord
    }

    def deleteRecordsForScheduledExecution(ScheduledExecution job) {
        JobFileRecord.findAllByJobId(job.extid).each this.&deleteRecord
    }

    def deleteRecordsForProject(String project) {
        JobFileRecord.findAllByProject(project).each this.&deleteRecord
    }

    /**
     * Validate whether the file ref uuid can be used for the jobid and option
     * @param fileuuid
     * @param jobid
     * @param option
     * @return [valid: true/false, error: 'code', args: [...]]
     */
    def validateFileRefForJobOption(String fileuuid, String jobid, String option) {
        JobFileRecord jfr = findUuid(fileuuid)
        if (!jfr) {
            return [valid: false, error: 'notfound', args: [fileuuid, jobid, option]]
        }
        return validateJobFileRecordForJobOption(jfr, jobid, option)
    }

    def validateJobFileRecordForJobOption(JobFileRecord jfr, String jobid, String option) {
        if (!jfr) {
            return [valid: false, error: 'notfound', args: [null, jobid, option]]
        }
        if (jfr.jobId != jobid || jfr.recordName != option) {
            return [valid: false, error: 'invalid', args: [jfr.uuid, jobid, option]]
        }
        if (jfr.execution != null) {
            return [valid: false, error: 'inuse', args: [jfr.uuid, jfr.execution.id]]
        }
        if (!jfr.canBecomeRetained()) {
            return [valid: false, error: 'state', args: [jfr.uuid, jfr.fileState]]
        }
        [valid: true]
    }

    /**
     * Retrieve the file by reference, to a local temp file.  If the
     * file is already on local disk, it will be returned directly,
     * otherwise it will be retrieved from the plugin.
     * The file SHA will be compared to expected SHA
     * @param fileuuid
     * @return
     */
    JobFileRecord attachFileForExecution(String fileuuid, Execution execution, String option) {
        JobFileRecord jfr = findUuid(fileuuid)
        def validate = validateJobFileRecordForJobOption(jfr, execution.scheduledExecution.extid, option)

        if (!validate.valid) {
            if (validate.error in ['notfound', 'invalid']) {
                throw new FileUploadServiceException(
                        "File ref \"$fileuuid\" is not a valid for job ${execution.scheduledExecution.extid}, option " +
                                option
                )
            }
            if (validate.error in ['inuse']) {
                throw new FileUploadServiceException(
                        "File ref \"$fileuuid\" cannot be used because it was used for execution ${jfr.execution.id}"
                )
            }
            if (validate.error in ['state']) {
                throw new FileUploadServiceException(
                        "File ref \"$fileuuid\" cannot be used because it is in state: " + jfr.fileState
                )
            }
        }

        try {
            jfr.stateRetained()
        } catch (IllegalStateException e) {
            throw new FileUploadServiceException(
                    "File ref \"$fileuuid\" cannot be used because it is in state: " + jfr.fileState
            )
        }
        jfr.execution = execution
        jfr.save()
        taskService.cancel("expire:$jfr.uuid")
        jfr
    }

    /**
     * Retrieve the file by reference, to a local temp file.  If the
     * file is already on local disk, it will be returned directly,
     * otherwise it will be retrieved from the plugin.
     * The file SHA will be compared to expected SHA
     * @param fileuuid
     * @return
     */
    File retrieveFileForExecution(JobFileRecord record) {
        File file
        String shastring
        (file, shastring) = retrieveTempFileForExecution(record.storageReference)
        if (record.sha != shastring) {
            if (file.exists()) {
                file.delete()
            }
            throw new FileUploadServiceException(
                    "SHA check failed for $record.uuid, expected $record.sha, saw $shastring"
            )
        }
        file
    }

    String getFileSHA(final File file) {
        file.withInputStream { is ->
            SHAInputStream.getSHAString(is)
        }
    }

    List<JobFileRecord> findExpiredRecords(String serverUUID, Date expiretime = new Date()) {
        JobFileRecord.findAllByExpirationDateLessThanEqualsAndFileStateAndServerNodeUUID expiretime, 'temp', serverUUID
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

    List<JobFileRecord> findRecords(final String jobid, String recordType, Map params = null) {
        JobFileRecord.findAllByJobIdAndRecordType(jobid, recordType, params)
    }

    List<JobFileRecord> findRecords(final String jobid, String recordType, String fileState, Map params = null) {
        JobFileRecord.findAllByJobIdAndRecordTypeAndFileState(jobid, recordType, fileState, params)
    }

    int countRecords(final String jobid, String recordType, String fileState) {
        JobFileRecord.countByJobIdAndRecordTypeAndFileState(jobid, recordType, fileState)
    }

    JobFileRecord findRecord(final String reference) {
        JobFileRecord.findByStorageReference(reference)
    }

    JobFileRecord findUuid(final String uuid) {
        JobFileRecord.findByUuid(uuid)
    }

    /**
     * Retrieve the file by reference, to a local temp file.  If the
     * file is already on local disk, it will be returned directly,
     * otherwise it will be retrieved from the plugin
     * @param reference
     * @return [file , "sha"]
     */
    def retrieveTempFileForExecution(String reference) {
        def plugin = getPlugin()
        File file
        try {
            file = plugin.retrieveLocalFile(reference)
            if (file) {
                return [file, getFileSHA(file)]
            }
        } catch (IOException e) {
            if (file?.exists()) {
                file.delete()
            }
            throw new FileUploadServiceException("Failed to retrieve file $reference: " + e, e)
        }
        //copy locally
        if (!plugin.hasFile(reference)) {
            throw new FileUploadServiceException("File is not available: $reference")
        }
        file = Files.createTempFile(reference, "tmp").toFile()
        file.deleteOnExit()
        String shastring
        try {
            file.withOutputStream {
                def shastream = new SHAOutputStream(it)
                plugin.retrieveFile(reference, shastream)
                shastring = shastream.SHAString
            }
        } catch (IOException e) {
            if (file.exists()) {
                file.delete()
            }
            throw new FileUploadServiceException("Failed to retrieve file $reference: " + e, e)
        }
        saveLocalReference(file, reference)
        [file, shastring]
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
                JobFileRecord jfr = attachFileForExecution(key, execution, it.name)
                File file = retrieveFileForExecution(jfr)
                loadedFiles[it.name] = file.absolutePath
                loadedFiles[it.name + '.fileName'] = jfr.fileName
                loadedFiles[it.name + '.sha'] = jfr.sha
                context.executionListener.log(3, "Retrieved file $key for option ${it.name} to $file.absolutePath")
            }
        }

        loadedFiles
    }

    /**
     * Attach file records to the execution for matching option values
     * @param execution execution
     * @param scheduledExecution job
     * @param optionInput input options mape
     * @return map of [optionName: filepath] for each loaded local file
     */
    void attachFileOptionInputs(
            Execution execution,
            ScheduledExecution scheduledExecution,
            Map<String, String> optionInput
    )
    {
        def fileopts = scheduledExecution.listFileOptions()
        fileopts?.each {
            def key = optionInput[it.name]
            if (key) {
                JobFileRecord jfr = attachFileForExecution(key, execution, it.name)
            }
        }

    }

    /**
     * Before execution starts, load any file inputs for the job from storage
     * @param evt
     * @return
     */
    @Subscriber
    def executionBeforeStart(ExecutionPrepareEvent evt) {
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
            return evt.context
        }
        null
    }

    /**
     * Before adhoc execution is scheduled in scheduler,
     * attach any declared files for the execution so that they will not be expired
     * @param evt
     * @return
     */
    def executionBeforeSchedule(ExecutionPrepareEvent evt) {
        if (evt.job && evt.execution && evt.options) {
            log.debug("executionBeforeSchedule($evt)")
            //handle uploaded files
            attachFileOptionInputs(evt.execution, evt.job, evt.options)
        }
    }
    /**
     * Remove temp files
     * @param event
     */
    @Subscriber
    def executionComplete(ExecutionCompleteEvent e) {
        JobFileRecord.withNewSession {
            findRecords(e.execution, RECORD_TYPE_OPTION_INPUT)?.each {
                changeFileState(it, FileUploadPlugin.ExternalState.Used)
            }
        }
    }

    Map<String, File> localFileMap = [:]


    private void removeLocalFile(String reference) {
        def file = localFileMap.remove(reference)
        if (file && file.exists()) {
            file.delete()
        }
    }

    private void saveLocalReference(File file, String reference) {
        localFileMap[reference] = file
    }
}

class FileUploadServiceException extends Exception {
    FileUploadServiceException() {
    }

    FileUploadServiceException(final String var1) {
        super(var1)
    }

    FileUploadServiceException(final String var1, final Throwable var2) {
        super(var1, var2)
    }

    FileUploadServiceException(final Throwable var1) {
        super(var1)
    }

    FileUploadServiceException(final String var1, final Throwable var2, final boolean var3, final boolean var4) {
        super(var1, var2, var3, var4)
    }
}
