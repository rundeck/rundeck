package rundeck.services.asyncimport

import com.dtolabs.rundeck.app.support.ProjectArchiveParams
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.IRundeckProject
import grails.converters.JSON
import grails.events.EventPublisher
import grails.events.annotation.Subscriber
import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Propagation
import rundeck.services.ConfigurationService
import rundeck.services.FrameworkService
import rundeck.services.ProjectService

import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.function.Predicate
import java.util.stream.Collectors
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Service that handles all the Asynchronous import functionality.
 *
 */
class AsyncImportService implements AsyncImportStatusFileOperations, EventPublisher {

    FrameworkService frameworkService
    ProjectService projectService
    ConfigurationService configurationService
    static Logger logger = LoggerFactory.getLogger(this.class)

    // Constants
    static final String TEMP_DIR = stripSlashFromString(System.getProperty("java.io.tmpdir"))
    static final Path BASE_WORKING_DIR = Paths.get(TEMP_DIR + File.separator + "AImport-WD-")
    static final String DISTRIBUTED_EXECUTIONS_FILENAME = "distributed_executions"
    static final String TEMP_PROJECT_SUFFIX = 'AImportTMP-'
    static final String JSON_FILE_PREFIX = 'AImport-status-'
    static final String JSON_FILE_EXT = '.json'
    static final String EXECUTION_DIR_NAME = 'executions'
    static final String MODEL_PROJECT_NAME_SUFFIX = 'rundeck-model-project'
    static final String MODEL_PROJECT_NAME_EXT = '.jar'
    static final String MODEL_PROJECT_INTERNAL_PREFIX = 'rundeck-'
    static final String MAX_EXECS_PER_DIR_PROP_NAME = "asyncImportConfig.maxDistributedExecutions"
    static final int KILOBYTE=1024

    static final String EXECUTION_FILE_PREFIX = 'execution-'
    static final String EXECUTION_FILE_EXT = '.xml'
    static final String OUTPUT_FILE_PREFIX = 'output-'
    static final String OUTPUT_FILE_EXT = '.rdlog'
    static final String STATE_FILE_PREFIX = 'state-'
    static final String STATE_FILE_EXT = '.state.json'

    /**
     *
     * Creates the status file that will be the main report to inform about the whole process,
     * a project cannot have more than a single status file.
     *
     * The file will be stored in project's file storage (Storage table in db)
     *
     * @param projectName - Required param to project.
     *
     * @return Boolean - "true" if the status file is created.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    Boolean createStatusFile(String projectName) {
        try {
            if( !statusFileExists(projectName) ){
                saveAsyncImportStatusForProject(projectName)
                return true
            }
            return false
        } catch (IOException e) {
            logger.error(e.stackTrace.toString())
            throw e
        }
    }

    /**
     * True/false if status file exists in DB.
     *
     * @param projectName
     * @return
     */
    Boolean statusFileExists(String projectName){
        try {
            def fwkProject = frameworkService.getFrameworkProject(projectName)
            def statusFilepath = "${JSON_FILE_PREFIX}${projectName}${JSON_FILE_EXT}"
            if( !fwkProject.existsFileResource(statusFilepath) ){
                return false
            }
            return true
        } catch (Exception e) {
            logger.error(e.stackTrace.toString())
            throw e
        }
    }

    /**
     *
     * Gets the status file content as an object.
     *
     * @param projectName - Required param to project.
     *
     * @return AsyncImportStatusDTO - DTO with all the status file content.
     */
    @Override
    AsyncImportStatusDTO getAsyncImportStatusForProject(String projectName) {
        try(def out = new ByteArrayOutputStream()){
            final def fwkProject = frameworkService.getFrameworkProject(projectName)
            fwkProject.loadFileResource(JSON_FILE_PREFIX + projectName + JSON_FILE_EXT, out)
            AsyncImportStatusDTO obj = new JsonSlurper().parseText(out.toString()) as AsyncImportStatusDTO
            logger.debug("Async project import status file content: ${obj.toString()}")
            return obj
        }catch(Exception e){
            throw new AsyncImportException("Errors getting the import status file for project: ${projectName}", e)
        }
    }

    /**
     * Just append two strings formatted for errors output
     *
     * @param oldErrors
     * @param newErrors
     * @return
     */
    static String appendErrorsInStatus(String oldErrors, String newErrors){
        try{
            if( oldErrors != null ){
                return "${oldErrors}, ${newErrors}"
            }
            return newErrors
        }catch(Exception e){
            throw new AsyncImportException("Error appending errors to old errors in project status file: ", e)
        }
    }

    /**
     * Saves a new or update a status file in db. To create a new status file, pass a project name, to update one,
     * pass a new AsyncImportStatusDTO with null unchanged properties, leaving only the updated properties non-null.
     *
     * @param projectName (nullable)
     * @param newStatus (nullable)
     * @return Long bytes written.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Long saveAsyncImportStatusForProject(String projectName, AsyncImportStatusDTO newStatus = null){
        def resource
        def statusPersist

        try {
            if( newStatus != null ){ // update scenario
                if (!newStatus.projectName || newStatus.projectName.size() <= 0) {
                    logger.error("No project name provided in new status.")
                    throw new MissingPropertyException("No project name provided in new status.")
                }
                statusPersist = new AsyncImportStatusDTO(newStatus)
            }else{
                statusPersist = new AsyncImportStatusDTO(
                        projectName,
                        AsyncImportMilestone.M1_CREATED.milestoneNumber
                ).with {
                    it.lastUpdated = new Date().toString()
                    it.lastUpdate = AsyncImportMilestone.M1_CREATED.name
                    return it
                }
            }
            def jsonStatus = statusPersist as JSON
            def inputStream = new ByteArrayInputStream(jsonStatus.toString().getBytes(StandardCharsets.UTF_8));
            final def fwkProject = frameworkService.getFrameworkProject(statusPersist.projectName)
            final def filename = JSON_FILE_PREFIX + statusPersist.projectName + JSON_FILE_EXT
            statusPersist.lastUpdated = new Date()
            resource = fwkProject.storeFileResource(filename, inputStream)
            inputStream.close();
            return resource
        } catch (Exception e) {
            throw new AsyncImportException("Error while saving the async project import status file in db", e)
        }
    }

    /**
     * Async import process Milestone 1: Transaction Requirements.
     * This method is synchronous, it validates the requirements to start the whole operation:
     *
     * a) Upload archive copy in /tmp without errors (rundeck server)
     * b) Working dir creation, which is the target dir in which all the executions and required files will be
     * moved to be worked on later
     * c) Creation of the "model project", a project in wich every single executions partition will be copied and
     * then zipped to be sent to "import to project" method.
     *
     * @param projectName - String
     * @param authContext - AuthContext
     * @param project - IRundeckProject
     * @param inputStream - InputStream (zipped project)
     * @param options - Import params
     * @return result - Import results
     */
    @Subscriber(AsyncImportEvents.ASYNC_IMPORT_EVENT_MILESTONE_1)
    def startAsyncImport(
            final String projectName,
            AuthContext authContext,
            IRundeckProject project,
            InputStream inputStream,
            ProjectArchiveParams options
    ){
        def updatedStatus = getAsyncImportStatusForProject(projectName)
        if( updatedStatus == null ){
            throw new AsyncImportException("No project import status file in DB for project: ${projectName}")
        }
        final def importExecutions = options.importExecutions
        def executionsDirFound = true

        logger.debug("Creating required directories.")

        updatedStatus.lastUpdate = "Creating required directories."
        saveAsyncImportStatusForProject(projectName,updatedStatus)

        def importResult = [:]

        String destDir = "${TEMP_DIR}${File.separator}${TEMP_PROJECT_SUFFIX}${projectName}"

        logger.debug("Creating a copy of the uploaded project in /tmp, in path: ${destDir}.")

        updatedStatus.lastUpdate = "Creating a copy of the uploaded project in /tmp."
        saveAsyncImportStatusForProject(projectName,updatedStatus)

        createProjectCopy(Paths.get(destDir.toString()), inputStream)

        updatedStatus.tempFilepath = destDir
        saveAsyncImportStatusForProject(projectName,updatedStatus)

        logger.debug("Creating the working directory in /tmp.")

        updatedStatus.lastUpdate = "Creating the working directory in /tmp."
        saveAsyncImportStatusForProject(projectName,updatedStatus)

        String scopedWorkingDir = "${BASE_WORKING_DIR}${projectName}"
        File baseWorkingDir = new File(scopedWorkingDir)
        File modelProjectHost = new File(baseWorkingDir.toString() + File.separator + MODEL_PROJECT_NAME_SUFFIX)
        createDirs(List.of(
                baseWorkingDir,
                modelProjectHost
        ))

        logger.debug("Creating the project model inside working directory in /tmp.")

        updatedStatus.lastUpdate = "Creating the project model inside working directory in /tmp."
        saveAsyncImportStatusForProject(projectName,updatedStatus)

        def framework = frameworkService.rundeckFramework
        Predicate<? super Path> internalProjectMatcher = path -> path.fileName.toString().startsWith(MODEL_PROJECT_INTERNAL_PREFIX)

        try {
            if( modelProjectHost.list().size() == 0 ){

                logger.debug("Checking if there are executions in project.")

                // check if the executions dir exists, if not, false a flag to prevent M2 trigger
                def internalProjectPath = getPathWithLogic(Paths.get(destDir.toString()), internalProjectMatcher)
                if( !checkExecutionsExistenceInPath(internalProjectPath) ){
                    executionsDirFound = false
                }
                copyDirExcept(destDir, modelProjectHost.toString(), EXECUTION_DIR_NAME)
            }

            def internalProjectInModelPath = getPathWithLogic(Paths.get(modelProjectHost.toString()), internalProjectMatcher)

            String newProjectName = "${MODEL_PROJECT_INTERNAL_PREFIX}${projectName}"

            if( internalProjectInModelPath != null ){
                Files.move(internalProjectInModelPath, Paths.get(modelProjectHost.toString()).resolve("${newProjectName}"))
            }

            String zippedFilename = "${baseWorkingDir.toString()}${File.separator}${projectName}${MODEL_PROJECT_NAME_EXT}"

            if( !Files.exists(Paths.get(zippedFilename)) ){
                try(FileOutputStream fos = new FileOutputStream(zippedFilename)){

                    ZipOutputStream zos = new ZipOutputStream(fos)

                    logger.debug("Ziping uploaded project.")

                    zipDir(modelProjectHost.toString(), "", zos)

                }catch(IOException ignored){
                    logger.error(ignored.stackTrace.toString())
                    throw ignored
                }
            }

            logger.debug("Uploading project jobs and config.")

            updatedStatus.lastUpdate = "Uploading project w/o executions."
            saveAsyncImportStatusForProject(projectName,updatedStatus)

            new FileInputStream(zippedFilename).withCloseable {fis ->{
                try {
                    importResult = projectService.importToProject(
                            project,
                            framework,
                            authContext as UserAndRolesAuthContext,
                            fis,
                            options
                    )
                }catch (IOException e){
                    throw new AsyncImportException("Errors uploading the file", e)
                }
            }}

            String jobUuidOption = options.jobUuidOption

            updatedStatus.jobUuidOption = jobUuidOption
            saveAsyncImportStatusForProject(projectName,updatedStatus)

            Files.delete(Paths.get(zippedFilename))

            if( !importResult.success ){

                logger.debug("Errors while importing the project w/o executions.")

                updatedStatus.lastUpdate = "Errors while importing the project w/o executions."
                saveAsyncImportStatusForProject(projectName,updatedStatus)

                deleteNonEmptyDir(destDir.toString())
                deleteNonEmptyDir(scopedWorkingDir)
                return importResult
            }

            updatedStatus.lastUpdate = "Cleaning the model project."
            saveAsyncImportStatusForProject(projectName,updatedStatus)

            List<Path> filepathsToRemove = Files.list(Paths.get("${modelProjectHost.toString()}${File.separator}${newProjectName}")).filter {
                it -> it.fileName.toString() != "jobs"
            }.collect(Collectors.toList())

            filepathsToRemove.forEach {
                it ->
                    {
                        File file = new File(it.toString())
                        if (file.isDirectory()) {
                            deleteNonEmptyDir(file.toString())
                        } else {
                            Files.delete(it)
                        }
                    }
            }

            updatedStatus.lastUpdate = "Phase 1 completed, calling phase 2 in process..."
            saveAsyncImportStatusForProject(projectName,updatedStatus)

            if( importExecutions && executionsDirFound ){
                projectService.beginAsyncImportMilestone(
                        projectName,
                        authContext,
                        project,
                        AsyncImportMilestone.M2_DISTRIBUTION.milestoneNumber
                )
            }else{
                // End the async import
                logger.debug("Async import process ended.")
                if( Files.exists(Paths.get(destDir)) ) deleteNonEmptyDir(destDir)
                if( Files.exists(Paths.get(scopedWorkingDir)) ) deleteNonEmptyDir(scopedWorkingDir)
                updatedStatus.lastUpdate = "All Executions uploaded, async import ended. Please check the target project."
                updatedStatus.milestone = AsyncImportMilestone.ASYNC_IMPORT_COMPLETED.name
                updatedStatus.milestoneNumber = AsyncImportMilestone.ASYNC_IMPORT_COMPLETED.milestoneNumber
                saveAsyncImportStatusForProject(projectName,updatedStatus)
            }

        } catch (Exception e) {
            throw new AsyncImportException("Errors starting the async import", e)
        }

        return importResult
    }


    /**
     * Async import process Milestone 2: Files Distribution.
     * This method is asynchronous and will only be triggered by the completion of milestone 1 through an event emission,
     * it:
     * 1) Creates the distributed executions dir in working directory for project
     * 2) Extracts the project copy in temp for a given project and list each execution of the project, then moves the executions
     * and its corresponding files to a bundle, which will have a maximum of 1000 executions per bundle or by the number passed to the prop
     * "rundeck.asyncImportConfig.maxDistributedExecutions". If the max is reached a next bundle will be created
     * 2) Removes the copy of the uploaded project in /tmp path
     * 3) Calls for the Milestone 3.
     *
     *
     * @param projectName - String
     * @param authContext - AuthContext
     * @param project - IRundeckProject
     */
    @Subscriber(AsyncImportEvents.ASYNC_IMPORT_EVENT_MILESTONE_2)
    def distributeExecutionFiles(
            final String projectName,
            AuthContext authContext,
            IRundeckProject project
    ){
        def updatedStatus = getAsyncImportStatusForProject(projectName)
        if( updatedStatus == null ){
            throw new AsyncImportException("No project import status file found in DB for project: ${projectName}")
        }

        final def milestoneNumber = AsyncImportMilestone.M2_DISTRIBUTION.milestoneNumber

        logger.debug("Starting to distribute executions.")

        updatedStatus.lastUpdate = "Starting to distribute executions."
        saveAsyncImportStatusForProject(projectName,updatedStatus)

        logger.debug("Creating executions host dir.")

        File baseWorkingDirToFile = new File(BASE_WORKING_DIR.toString() + projectName)
        File distributedExecutions = new File(baseWorkingDirToFile.toString() + File.separator + DISTRIBUTED_EXECUTIONS_FILENAME)
        if( baseWorkingDirToFile.exists() ){
            distributedExecutions.mkdir()
        }

        updatedStatus.lastUpdate = "Extracting TMP filepath from project import status file."
        saveAsyncImportStatusForProject(projectName,updatedStatus)

        logger.debug("Extracting TMP location via project import status file.")

        File tempFile = new File(updatedStatus.tempFilepath)
        if( !tempFile.exists() ){
            throw new AsyncImportException("Unable to locate temp project during Milestone 2, please restart the process in other new project and delete current.")
        }

        Predicate<? super Path> internalProjectMatcher = path -> path.fileName.toString().startsWith(MODEL_PROJECT_INTERNAL_PREFIX)
        Path rundeckInternalProjectPath = getPathWithLogic(Paths.get(tempFile.toString()), internalProjectMatcher)

        File executionsDir = new File(rundeckInternalProjectPath.toString() + File.separator + EXECUTION_DIR_NAME)

        logger.debug("Listing executions and corresponding filepaths.")

        updatedStatus.lastUpdate = "Listing executions and corresponding filepaths."
        saveAsyncImportStatusForProject(projectName,updatedStatus)

        List<Path> xmls = getFilesPathsByPrefixAndExtensionInPath(executionsDir.toString(), EXECUTION_FILE_PREFIX, EXECUTION_FILE_EXT)
        List<Path> logs = getFilesPathsByPrefixAndExtensionInPath(executionsDir.toString(), OUTPUT_FILE_PREFIX, OUTPUT_FILE_EXT)
        List<Path> state = getFilesPathsByPrefixAndExtensionInPath(executionsDir.toString(), STATE_FILE_PREFIX, STATE_FILE_EXT)

        logger.debug("Total executions found: ${xmls.size()}, total log files found: ${logs.size()}, total state files found: ${state.size()}")

        updatedStatus.lastUpdate = "Total executions found: ${xmls.size()}, total log files found: ${logs.size()}, total state files found: ${state.size()}"
        saveAsyncImportStatusForProject(projectName,updatedStatus)

        long distributedExecutionBundles = Files.walk(Paths.get(executionsDir.toString()), FileVisitOption.FOLLOW_LINKS)
                .filter(Files::isDirectory)
                .filter(path -> path.getFileName().toString().matches("\\d+"))
                .sorted(Comparator.comparingInt(path -> Integer.parseInt(path.getFileName().toString())))
                .count()

        File distributedExecutionBundle = null

        if( distributedExecutionBundles == 0 ){
            distributedExecutionBundle = new File(distributedExecutions.toString() + File.separator + "1")
            distributedExecutionBundle.mkdir()
        }

        logger.debug("Beginning files iteration...")

        updatedStatus.lastUpdate = "Beginning files iteration..."
        saveAsyncImportStatusForProject(projectName,updatedStatus)

        if (xmls.size() > 0) {
            try {
                xmls.forEach { execution ->
                    String trimmedExecutionSerial = execution.fileName.toString()
                            .replace(EXECUTION_FILE_PREFIX, "")
                            .replace(EXECUTION_FILE_EXT, "")
                            .trim()

                    // If there are less than <max execs> in bundle, move the exes and files to bundle
                    List<Path> xmlInBundle = getFilesPathsByPrefixAndExtensionInPath(
                            distributedExecutionBundle.toString(),
                            EXECUTION_FILE_PREFIX as String,
                            EXECUTION_FILE_EXT as String)

                    def maxExecutionsPerDir = configurationService.getInteger(MAX_EXECS_PER_DIR_PROP_NAME, 1000)

                    if (xmlInBundle.size() == maxExecutionsPerDir) {
                        //get the bundle name to int to increase the next bundle
                        int previousBundleNameToInt = Integer.parseInt(distributedExecutionBundle.name)
                        File newExecutionBundle = new File(String.valueOf(distributedExecutions.toString() + File.separator + (previousBundleNameToInt + 1)))
                        newExecutionBundle.mkdir()
                        distributedExecutionBundle = newExecutionBundle
                    }
                    // if the execution has logs, move the file
                    // if the execution has state, move the file
                    // move the execution
                    Optional<Path> logFound = logs.stream()
                            .filter { log -> log.toString().contains(trimmedExecutionSerial) }
                            .findFirst()
                    Optional<Path> stateFound = state.stream()
                            .filter { stateFile -> stateFile.toString().contains(trimmedExecutionSerial) }
                            .findFirst()
                    Path distributedExecutionsPath = Paths.get(distributedExecutionBundle.toString())

                    updatedStatus.lastUpdate = "Moving file: #${trimmedExecutionSerial} of ${xmls.size()}."
                    saveAsyncImportStatusForProject(projectName,updatedStatus)

                    if (logFound.isPresent()) {
                        //move it
                        if( Files.exists(logFound.get()) ){
                            Files.move(logFound.get(), distributedExecutionsPath.resolve(logFound.get().fileName), StandardCopyOption.REPLACE_EXISTING)
                        }
                    }
                    if (stateFound.isPresent()) {
                        //move it
                        if( Files.exists(stateFound.get()) ){
                            Files.move(stateFound.get(), distributedExecutionsPath.resolve(stateFound.get().fileName), StandardCopyOption.REPLACE_EXISTING)
                        }
                    }
                    // move the execution
                    if( Files.exists(execution) ){
                        Files.move(execution, distributedExecutionsPath.resolve(execution.fileName), StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            } catch (Exception e) {
                logger.error(e.stackTrace.toString())
                throw e
            }

            deleteNonEmptyDir(tempFile.toString())

            logger.debug("Executions distributed; proceeding to call files upload event.")

            updatedStatus.lastUpdate = "Executions distributed; proceeding to call files upload event."
            saveAsyncImportStatusForProject(projectName,updatedStatus)

            projectService.beginAsyncImportMilestone(
                    projectName,
                    authContext,
                    project,
                    AsyncImportMilestone.M3_IMPORTING.milestoneNumber
            )

        }else{
            logger.debug("No executions to iterate, asynchronous import process ended.")
            updatedStatus.lastUpdate = "No executions to iterate, asynchronous import process ended."
            updatedStatus.milestoneNumber = AsyncImportMilestone.ASYNC_IMPORT_COMPLETED.milestoneNumber
            updatedStatus.milestone = AsyncImportMilestone.ASYNC_IMPORT_COMPLETED.name
            updatedStatus.milestoneNumber = AsyncImportMilestone.ASYNC_IMPORT_COMPLETED.milestoneNumber
            saveAsyncImportStatusForProject(projectName,updatedStatus)
        }
    }

    /**
     * Async import process Milestone 3: Files Import
     * This method is asynchronous and only will be triggered by the completion of milestone 2 through an event emission,
     * it:
     * a) Iterates the executions bundles in distributed_executions path of the working directory and for each one:
     *  1) Copy each bundle to the model project in the working directory
     *  2) Zip the model project and parse it as an input stream to feed "importToProject"
     *  3) Calls "importToProject"
     *  4) Remove the zip and start over until the last bundle is imported.
     *
     * @param projectName - String
     * @param authContext - AuthContext
     * @param project - IRundeckProject
     */
    @Subscriber(AsyncImportEvents.ASYNC_IMPORT_EVENT_MILESTONE_3)
    def uploadBundledExecutions(
            final String projectName,
            AuthContext authContext,
            IRundeckProject project
    ){
        def updatedStatus = getAsyncImportStatusForProject(projectName)
        if( updatedStatus == null ){
            throw new AsyncImportException("No project import status file found in DB for project: ${projectName}")
        }
        final def milestoneNumber = AsyncImportMilestone.M3_IMPORTING.milestoneNumber

        logger.debug("Files upload operation started....")

        if( !projectName ){
            throw new MissingPropertyException("No project name passed in event.")
        }

        updatedStatus.lastUpdate = "Files upload operation started...."
        saveAsyncImportStatusForProject(projectName,updatedStatus)

        def framework = frameworkService.rundeckFramework

        def jobUuidOption = getAsyncImportStatusForProject(projectName).jobUuidOption

        def options = [
                jobUuidOption     :jobUuidOption,
                importExecutions  : true
        ] as ProjectArchiveParams

        try {
            logger.debug("Iterating execution bundles.")

            updatedStatus.lastUpdate = "Iterating execution bundles."
            saveAsyncImportStatusForProject(projectName,updatedStatus)

            def distributedExecutionsFullPath = Paths.get("${BASE_WORKING_DIR.toString()}${projectName}${File.separator}${DISTRIBUTED_EXECUTIONS_FILENAME}")
            Path firstDir
            def executionBundles = null
            try {
                executionBundles = getExecutionBundles(distributedExecutionsFullPath)

                if( executionBundles.size() ){

                    logger.debug("A total of ${executionBundles.size()} execution bundles found, iterating in progress..")

                    updatedStatus.lastUpdate = "A total of ${executionBundles.size()} execution bundles found, iterating in progress.."
                    saveAsyncImportStatusForProject(projectName,updatedStatus)

                    executionBundles.forEach{ bundle ->
                        if (!Files.exists(bundle) || !Files.isDirectory(bundle)) {
                            throw new AsyncImportException("Bundle corrupted or not a directory.")
                        }
                        firstDir = bundle
                        def modelProjectFullPath = Paths.get("${BASE_WORKING_DIR.toString()}${projectName}${File.separator}${MODEL_PROJECT_NAME_SUFFIX}")
                        def modelProjectExecutionsContainerPath = Paths.get("${modelProjectFullPath}${File.separator}${MODEL_PROJECT_INTERNAL_PREFIX}${projectName}")
                        try {
                            try {
                                Files.move(firstDir, modelProjectExecutionsContainerPath.resolve(EXECUTION_DIR_NAME), StandardCopyOption.REPLACE_EXISTING)
                            } catch (NoSuchFileException ignored) {
                                logger.error(ignored.stackTrace.toString())
                                throw ignored
                            }

                            def zippedFilename = "${BASE_WORKING_DIR}${projectName}${File.separator}${firstDir.fileName}${MODEL_PROJECT_NAME_EXT}"

                            try(FileOutputStream fos = new FileOutputStream(zippedFilename)){

                                ZipOutputStream zos = new ZipOutputStream(fos)

                                zipDir(modelProjectFullPath.toString(), "", zos)

                            }catch(IOException ignored){
                                logger.error(ignored.stackTrace.toString())
                                throw ignored
                            }

                            def result

                            updatedStatus.lastUpdate = "Uploading execution bundle #${firstDir.fileName}, ${executionBundles.size() - 1} bundles remaining."
                            saveAsyncImportStatusForProject(projectName,updatedStatus)

                            new FileInputStream(zippedFilename).withCloseable { fis -> {
                                try {
                                    result = projectService.importToProject(
                                            project,
                                            framework,
                                            authContext as UserAndRolesAuthContext,
                                            fis,
                                            options
                                    )
                                } catch (IOException e) {
                                    logger.error(e.stackTrace.toString())
                                    throw e
                                }
                            }}

                            if (result.success) {
                                def modelProjectExecutionsContainerFullPath = Paths.get("${modelProjectExecutionsContainerPath}${File.separator}${EXECUTION_DIR_NAME}")
                                try {
                                    if (Files.exists(modelProjectExecutionsContainerFullPath) && Files.isDirectory(modelProjectExecutionsContainerFullPath)) {
                                        deleteNonEmptyDir(modelProjectExecutionsContainerFullPath.toString())
                                    } else {
                                        throw new FileNotFoundException("Executions directory don't exist or is not a directory.")
                                    }
                                    if (Files.exists(Paths.get(zippedFilename))) {
                                        Files.delete(Paths.get(zippedFilename))
                                    } else {
                                        throw new FileNotFoundException("Zipped model project not found.")
                                    }
                                } catch (Exception e) {
                                    updatedStatus.errors = appendErrorsInStatus(updatedStatus.errors, e.stackTrace.toString())
                                    saveAsyncImportStatusForProject(projectName, updatedStatus)
                                    throw e
                                }
                            }
                            if (result.execerrors) {
                                updatedStatus.errors = appendErrorsInStatus(updatedStatus.errors, "${result.execerrors?.toString()}")
                                saveAsyncImportStatusForProject(projectName, updatedStatus)
                            }
                        } catch (Exception e) {
                            updatedStatus.errors = appendErrorsInStatus(updatedStatus.errors, e.stackTrace.toString())
                            saveAsyncImportStatusForProject(projectName, updatedStatus)
                        }
                    }
                }else{
                    logger.debug("No execution bundles to iterate, ending async import process")
                }

                deleteNonEmptyDir("${BASE_WORKING_DIR.toString()}${projectName}")

                logger.debug("All Executions uploaded, async import ended. Please check the target project.")

                updatedStatus.milestone = AsyncImportMilestone.ASYNC_IMPORT_COMPLETED.name
                updatedStatus.milestoneNumber = AsyncImportMilestone.ASYNC_IMPORT_COMPLETED.milestoneNumber
                updatedStatus.lastUpdate = "All Executions uploaded, async import ended. Please check the target project."
                saveAsyncImportStatusForProject(projectName, updatedStatus)

            } catch (IOException e) {
                // Report the error
                updatedStatus.errors = appendErrorsInStatus(updatedStatus.errors, e.stackTrace.toString())
                saveAsyncImportStatusForProject(projectName, updatedStatus)
            }
        } catch (IOException e) {
            // Report the error
            updatedStatus.errors = appendErrorsInStatus(updatedStatus.errors, e.stackTrace.toString())
            saveAsyncImportStatusForProject(projectName, updatedStatus)
        }
    }

    /**
     * Takes a list of files and creates them
     *
     * @param filesToCreate
     * @return
     */
    static createDirs(List<File> filesToCreate) {
        try {
            if (filesToCreate.size()) {
                filesToCreate.each { file ->
                    file.mkdirs()
                }
            }
        } catch (Exception e) {
            logger.error(e.stackTrace.toString())
        }
    }

    /**
     * Checks executions in given exported project dir path
     *
     * @param internalProjectPath
     * @return
     */
    static boolean checkExecutionsExistenceInPath(Path internalProjectPath) {
        try{
            Predicate<? super Path> executionsDirMatcher = path -> path.fileName.toString() == EXECUTION_DIR_NAME
            def executionsDirPath = getPathWithLogic(internalProjectPath, executionsDirMatcher)
            if (executionsDirPath == null) {
                return false
            } else {
                // If the dir exists but there are no executions, dont start M2
                def hasExecutions = Files.list(executionsDirPath).count()
                if (hasExecutions <= 0) {
                    return false
                }
            }
        }catch(Exception e){
            logger.error(e.stackTrace.toString())
        }
        return true
    }

    /**
     * Zips recursively a directory
     *
     * @param unzippedFilepath - the filepath in which the zip-to-be dir is
     * @param zippedFilePath - the path in which we want to have the zipped dir
     * @param zos - output stream
     * @throws IOException
     */
    static void zipDir(String unzippedFilepath, String zippedFilePath, ZipOutputStream zos) throws IOException {
        File dir = new File(unzippedFilepath)
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                zipDir(file.getAbsolutePath(), zippedFilePath + file.getName() + "/", zos)
            } else {
                try (FileInputStream fis = new FileInputStream(file)) {
                    ZipEntry zipEntry = new ZipEntry(zippedFilePath + file.getName())
                    zos.putNextEntry(zipEntry)
                    byte[] buffer = new byte[KILOBYTE]
                    int len
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len)
                    }
                    zos.closeEntry()
                }
            }
        }
    }

    /**
     * Deletes non-empty dir recursively
     *
     * @param path
     */
    static void deleteNonEmptyDir(String path){
        try {
            FileUtils.deleteDirectory(new File(path))
        } catch (IOException e) {
            logger.error(e.stackTrace.toString())
        }
    }

    /**
     * Parses a input stream as a dir and copy it to given path.
     *
     * @param destinationDir
     * @param is
     * @return
     */
    static def createProjectCopy(Path destinationDir, InputStream is){
        if (!Files.exists(destinationDir)) {
            try {
                extractStream(destinationDir.toString(), is)
            } catch (Exception e) {
                logger.error(e.stackTrace.toString())
                throw e
            }
        }
    }

    /**
     * Takes a input stream and writes it like a directory in given destination dir
     *
     * @param destDir
     * @param inputStream
     */
    static void extractStream(String destDir, InputStream inputStream){
        new ZipInputStream(inputStream).withCloseable {zipInputStream -> {
            try {
                File checkDir = new File(destDir)
                if( !checkDir.exists() ){
                    checkDir.mkdirs()
                }
                ZipEntry zipEntry
                while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                    String newFileName = zipEntry.getName()
                    File destFile = new File(destDir, newFileName)

                    if (zipEntry.isDirectory()) {
                        destFile.mkdirs()
                    } else {
                        File parent = destFile.getParentFile();
                        if (!parent.exists()) {
                            parent.mkdirs()
                        }

                        FileOutputStream fos = new FileOutputStream(destFile);
                        byte[] buffer = new byte[KILOBYTE]
                        int bytesRead
                        while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead)
                        }
                        fos.close()
                    }
                    zipInputStream.closeEntry()
                }
            } catch (IOException e) {
                logger.error(e.stackTrace.toString())
            }
        }}
    }

    /**
     * Copies a directory recursively into a another except a string-matched dir and its content
     *
     * @param origin
     * @param target
     * @param ignored
     * @return
     */
    def copyDirExcept(String origin, String target, String ignored) {
        def originDir = new File(origin)
        def destDir = new File(target)
        def exclude = new File(originDir, ignored)
        def copyRecursively
        try{
            copyRecursively = { File originFile, File destFile ->
                if (originFile.name != exclude.name) {
                    if (originFile.isDirectory()) {
                        def destination = new File(destFile, originFile.name)
                        if (!destination.exists()) {
                            destination.mkdirs()
                        }
                        originFile.eachFile { File file ->
                            copyRecursively(file, destination)
                        }
                    } else {
                        FileUtils.copyFileToDirectory(originFile, destFile)
                    }
                }
            }

            if (originDir.exists() && originDir.isDirectory()) {
                destDir.mkdirs()
                originDir.eachFile { archivo ->
                    copyRecursively(archivo, destDir)
                }
            }
        }catch (Exception e){
            logger.error(e.stackTrace.toString())
        }
    }

    /**
     * Given file prefix and extention, returns the full path of matched files,
     * this method is handy when we seek inside a dir with files with the same name and different prefix/extensions.
     *
     * @param path
     * @param prefix
     * @param ext
     * @return
     */
    static List<Path> getFilesPathsByPrefixAndExtensionInPath(String path, String prefix, String ext){
        try{
            return Files.list(Paths.get(path.toString()))
                    .sorted((s1, s2) -> {
                        int num1 = Integer.parseInt(s1.fileName.toString().replaceAll("\\D", ""))
                        int num2 = Integer.parseInt(s2.fileName.toString().replaceAll("\\D", ""))
                        return Integer.compare(num1, num2)
                    })
                    .filter {
                        it -> {
                            it.fileName.toString().startsWith(prefix) && it.fileName.toString().endsWith(ext)
                        }
                    }.collect(Collectors.toList())
        }catch(Exception e){
            logger.error(e.stackTrace.toString())
            throw e
        }
    }

    /**
     * Strips the last slash from the temp dir path to allow the async importer to work in windows and linux
     *
     * @return a string w/o slash at the end
     */
    static String stripSlashFromString(String string){
        def tempProp = null
        def prop = string
        if( prop.endsWith('\\') ){
            def trimmedProp = prop.substring(0, prop.size() -1)
            tempProp = trimmedProp
        }else if(prop.endsWith('/')){
            def trimmedUnixProp = prop.substring(0, prop.size() -1)
            tempProp = trimmedUnixProp
        } else{
            tempProp = prop
        }
        return tempProp
    }

    /**
     * Given a path, iterates through the file tree and applies the logic passed as an argument,
     * if the logic matches, returns the first path that match.
     *
     * this method reduce some lines of code when we have to search in the file tree of a
     * dir for a path with different logic blocks.
     *
     * @param checked
     * @param logic
     * @return
     */
    static Path getPathWithLogic(Path checked, Predicate<? super Path> logic){
        Path path = null
        try{
            if( Files.list(checked).anyMatch(logic) ){
                path = Files.list(checked).filter(logic).findFirst().get()
            }
        }catch(Exception e){
            logger.error(e.stackTrace.toString())
        }
        return path
    }

    /**
     * Return the distributed executions child dirs paths sorted
     *
     * @param distributedExecutionsPath
     * @return
     */
    static List<Path> getExecutionBundles(Path distributedExecutionsPath){
        try{
            return Files.walk(distributedExecutionsPath, FileVisitOption.FOLLOW_LINKS)
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().matches("\\d+"))
                    .sorted(Comparator.comparingInt(path -> Integer.parseInt(path.getFileName().toString())))
                    .collect(Collectors.toList())
        }catch(IOException e){
            logger.error(e.stackTrace.toString())
            throw e
        }
    }

}
