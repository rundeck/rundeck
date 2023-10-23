package rundeck.services.asyncimport

import com.dtolabs.rundeck.app.support.ProjectArchiveParams
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.IRundeckProject
import grails.compiler.GrailsCompileStatic
import grails.converters.JSON
import grails.events.EventPublisher
import grails.events.annotation.Subscriber
import groovy.json.JsonSlurper
import org.apache.commons.io.FileUtils
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

    // Constants
    static final String TEMP_DIR = System.getProperty("java.io.tmpdir")
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
    @Override
    Boolean createStatusFile(String projectName) {
        try {
            if( !statusFileExists(projectName) ){
                saveAsyncImportStatusForProject(projectName)
                return true
            }
            return false
        } catch (IOException e) {
            e.printStackTrace();
            throw e
        }
    }

    private Boolean statusFileExists(String projectName){
        try {
            def fwkProject = frameworkService.getFrameworkProject(projectName)
            def statusFilepath = "${JSON_FILE_PREFIX}${projectName}${JSON_FILE_EXT}"
            if( !fwkProject.existsFileResource(statusFilepath) ){
                return false
            }
            return true
        } catch (Exception e) {
            e.printStackTrace();
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
    AsyncImportStatusDTO getAsyncImportStatusForProject(String projectName, ByteArrayOutputStream out = null) {
        try{
            if( out == null ){
                out = new ByteArrayOutputStream()
            }
            final def fwkProject = frameworkService.getFrameworkProject(projectName)
            fwkProject.loadFileResource(JSON_FILE_PREFIX + projectName + JSON_FILE_EXT, out)
            def obj = new JsonSlurper().parseText(out.toString()) as AsyncImportStatusDTO
            log.debug("Object extracted: ${obj.toString()}")
            return obj
        }catch(Exception e){
            log.error("Error during the async import file extraction process: ${e.message}")
            throw e
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
    Long saveAsyncImportStatusForProject(String projectName = null, AsyncImportStatusDTO newStatus = null){
        def resource
        def statusPersist

        if( !projectName && !newStatus ){
            throw new AsyncImportException("Must pass either a projectName or a new AsyncImportStatusDTO in method call.")
        }

        try {
            if( newStatus != null ){ // update scenario
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
            if (!statusPersist.projectName || statusPersist.projectName.size() <= 0) {
                log.error("No project name provided in new status.")
                throw new MissingPropertyException("No project name provided in new status.")
            }
            final def fwkProject = frameworkService.getFrameworkProject(statusPersist.projectName)
            final def filename = JSON_FILE_PREFIX + statusPersist.projectName + JSON_FILE_EXT
            resource = fwkProject.storeFileResource(filename, inputStream)
            inputStream.close();
            return resource
        } catch (IOException e) {
            e.printStackTrace();
            throw e
        }
    }

    /**
     * async import status file updater, it replace the non-null attributes of "newStatus" object in the
     * existing status file stored in db, leaving the null ones in the same object as they are in db.
     *
     * @param newStatus
     * @return Long (bytes written)
     */
    def asyncImportStatusFileUpdater( AsyncImportStatusDTO newStatus, ByteArrayOutputStream out = null ){
        if( out == null ){
            out = new ByteArrayOutputStream()
        }
        def oldStatus = getAsyncImportStatusForProject(newStatus.projectName, out)
        if( oldStatus == null ) throw new AsyncImportException("No status file for project: ${newStatus.projectName}")
        if( oldStatus.errors != null ){
            def oldStatusErrors = oldStatus.errors
            newStatus.errors = oldStatusErrors + ", " + newStatus.errors
        }
        AsyncImportStatusDTO.replacePropsInTargetDTO(newStatus, oldStatus)
        return saveAsyncImportStatusForProject(null, newStatus)
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
    def beginMilestone1(
            final String projectName,
            AuthContext authContext,
            IRundeckProject project,
            InputStream inputStream,
            ProjectArchiveParams options
    ){
        final def milestoneNumber = AsyncImportMilestone.M1_CREATED.milestoneNumber

        asyncImportStatusFileUpdater(new AsyncImportStatusDTO(projectName, milestoneNumber).with {
            it.lastUpdate = "Starting M1... Creating required directories."
            return it
        })

        def importResult = [:]

        String destDir = "${TEMP_DIR}${File.separator}${TEMP_PROJECT_SUFFIX}${projectName}"
        if (!Files.exists(Paths.get(destDir))) {
            try {
                asyncImportStatusFileUpdater(new AsyncImportStatusDTO(projectName, milestoneNumber).with {
                    it.lastUpdate = "Creating a copy of the uploaded project in /tmp."
                    return it
                })
                createTempCopyFromStream(destDir, inputStream)
                asyncImportStatusFileUpdater(new AsyncImportStatusDTO(projectName, milestoneNumber).with {
                    it.tempFilepath = destDir
                    return it
                })
            } catch (Exception e) {
                e.printStackTrace()
                throw e
            }
        }

        asyncImportStatusFileUpdater(new AsyncImportStatusDTO(projectName, milestoneNumber).with {
            it.lastUpdate = "Creating the working directory in /tmp."
            return it
        })

        String scopedWorkingDir = "${BASE_WORKING_DIR}${projectName}"
        File baseWorkingDir = new File(scopedWorkingDir)
        if (!baseWorkingDir.exists()) {
            baseWorkingDir.mkdir()
        }
        File modelProjectHost = new File(baseWorkingDir.toString() + File.separator + MODEL_PROJECT_NAME_SUFFIX)
        if (!modelProjectHost.exists()) {
            modelProjectHost.mkdir()
        }

        asyncImportStatusFileUpdater(new AsyncImportStatusDTO(projectName, milestoneNumber).with {
            it.lastUpdate = "Creating the project model inside working directory in /tmp."
            return it
        })

        def framework = frameworkService.rundeckFramework

        try {

            if( modelProjectHost.list().size() == 0 ){
                copyDirExcept(destDir, modelProjectHost.toString(), EXECUTION_DIR_NAME)
            }

            Optional<Path> dirFound = Files.list(Paths.get(modelProjectHost.toString()))
                                .filter {
                                    path -> path.fileName.toString().startsWith(MODEL_PROJECT_INTERNAL_PREFIX)
                                }
                                .findFirst()

            if( dirFound.isPresent() ){
                if( Files.exists(Paths.get(dirFound.get().toString())) ){
                    Files.move(Paths.get(dirFound.get().toString()), Paths.get(modelProjectHost.toString()).resolve("${MODEL_PROJECT_INTERNAL_PREFIX}${projectName}"))
                }
            }

            String zippedFilename = "${baseWorkingDir.toString()}${File.separator}${projectName}${MODEL_PROJECT_NAME_EXT}"

            if( !Files.exists(Paths.get(zippedFilename)) ){
                zipModelProject(modelProjectHost.toString(), zippedFilename)
            }

            FileInputStream fis = new FileInputStream(zippedFilename);

            asyncImportStatusFileUpdater(new AsyncImportStatusDTO(projectName, milestoneNumber).with {
                it.lastUpdate = "Uploading project w/o executions."
                return it
            })

            importResult = projectService.importToProject(
                    project,
                    framework,
                    authContext as UserAndRolesAuthContext,
                    fis,
                    options
            )

            String jobUuidOption = options.jobUuidOption

            asyncImportStatusFileUpdater(new AsyncImportStatusDTO(projectName, milestoneNumber).with {
                it.jobUuidOption = jobUuidOption
                return it
            })

            Files.delete(Paths.get(zippedFilename))

            if( !importResult.success ){
                asyncImportStatusFileUpdater(new AsyncImportStatusDTO(projectName, milestoneNumber).with {
                    it.lastUpdate = "Errors while importing the project w/o executions."
                    return it
                })
                deleteNonEmptyDir(destDir.toString())
                deleteNonEmptyDir(scopedWorkingDir)
                return importResult
            }
        } catch (Exception e) {
            e.printStackTrace()
            throw e
        }

        asyncImportStatusFileUpdater(new AsyncImportStatusDTO(projectName, milestoneNumber).with {
            it.lastUpdate = "Cleaning the model project."
            return it
        })

        Path pathToRundeckInternalProject = Files.list(Paths.get(modelProjectHost.toString()))
                .filter { it ->
                    it.fileName.toString().startsWith("rundeck-")
                }.collect(Collectors.toList())[0]

        List<Path> filepathsToRemove = Files.list(pathToRundeckInternalProject).filter {
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

        asyncImportStatusFileUpdater(new AsyncImportStatusDTO(projectName, milestoneNumber).with {
            it.lastUpdate = "Milestone 1 completed, calling Milestone 2 in process..."
            return it
        })

        projectService.beginAsyncImportMilestone(
                projectName,
                authContext,
                project,
                AsyncImportMilestone.M2_DISTRIBUTION.milestoneNumber
        )

        return importResult
    }


    /**
     * Async import process Milestone 2: Files Distribution.
     * This method is asynchronous and only will be triggered by the completion of milestone 1 through an event emission,
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
    def beginMilestone2(
            final String projectName,
            AuthContext authContext,
            IRundeckProject project
    ){
        final def milestoneNumber = AsyncImportMilestone.M2_DISTRIBUTION.milestoneNumber

        asyncImportStatusFileUpdater(new AsyncImportStatusDTO(projectName, milestoneNumber).with {
            it.lastUpdate = "Milestone 2 in process..."
            return it
        })

        File baseWorkingDirToFile = new File(BASE_WORKING_DIR.toString() + projectName)
        File distributedExecutions = new File(baseWorkingDirToFile.toString() + File.separator + DISTRIBUTED_EXECUTIONS_FILENAME)
        if( baseWorkingDirToFile.exists() ){
            distributedExecutions.mkdir()
        }

        asyncImportStatusFileUpdater(new AsyncImportStatusDTO(projectName, milestoneNumber).with {
            it.lastUpdate = "Extracting TMP filepath from status file."
            return it
        })

        AsyncImportStatusDTO statusFileForProject = getAsyncImportStatusForProject(projectName)
        if( statusFileForProject == null ){
            asyncImportStatusFileUpdater(new AsyncImportStatusDTO(projectName, milestoneNumber).with {
                it.errors = "No status file found in working dir for project: ${projectName}"
                return it
            })
            throw new AsyncImportException("No status file found in working dir for project: ${projectName}")
        }

        File tempFile = new File(statusFileForProject.tempFilepath)
        if( !tempFile.exists() ){
            throw new AsyncImportException("Unable to locate temp project during Milestone 2, please restart the process in other new project and delete current.")
        }

        Path rundeckInternalProjectPath = getInternalRundeckProjectPath(tempFile.toString())

        File executionsDir = new File(rundeckInternalProjectPath.toString() + File.separator + EXECUTION_DIR_NAME)

        asyncImportStatusFileUpdater(new AsyncImportStatusDTO(projectName, milestoneNumber).with {
            it.lastUpdate = "Listing executions and corresponding filepaths."
            return it
        })

        List<Path> xmls = getFilesPathsByPrefixAndExtensionInPath(executionsDir.toString(), EXECUTION_FILE_PREFIX, EXECUTION_FILE_EXT)
        List<Path> logs = getFilesPathsByPrefixAndExtensionInPath(executionsDir.toString(), OUTPUT_FILE_PREFIX, OUTPUT_FILE_EXT)
        List<Path> state = getFilesPathsByPrefixAndExtensionInPath(executionsDir.toString(), STATE_FILE_PREFIX, STATE_FILE_EXT)

        asyncImportStatusFileUpdater(new AsyncImportStatusDTO(projectName, milestoneNumber).with {
            it.lastUpdate = "Total executions found: ${xmls.size()}, total log files found: ${logs.size()}, total state files found: ${state.size()}"
            return it
        })

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

        asyncImportStatusFileUpdater(new AsyncImportStatusDTO(projectName, milestoneNumber).with {
            it.lastUpdate = "Beginning files iteration..."
            return it
        })

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

                    asyncImportStatusFileUpdater(new AsyncImportStatusDTO(projectName, milestoneNumber).with {
                        it.lastUpdate = "Moving file: #${trimmedExecutionSerial} of ${xmls.size()}."
                        return it
                    })

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
                e.printStackTrace()
                throw e
            }

            deleteNonEmptyDir(tempFile.toString())

            asyncImportStatusFileUpdater(new AsyncImportStatusDTO(projectName, milestoneNumber).with {
                it.lastUpdate = "Executions distributed, M2 done; proceeding to call M3 event."
                return it
            })

            projectService.beginAsyncImportMilestone(
                    projectName,
                    authContext,
                    project,
                    AsyncImportMilestone.M3_IMPORTING.milestoneNumber
            )

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
    def beginMilestone3(
            final String projectName,
            AuthContext authContext,
            IRundeckProject project
    ){

        final def milestoneNumber = AsyncImportMilestone.M3_IMPORTING.milestoneNumber

        asyncImportStatusFileUpdater(new AsyncImportStatusDTO(projectName, milestoneNumber).with {
            it.lastUpdate = "Milestone 3 started...."
            return it
        })

        if( !projectName ){
            throw new MissingPropertyException("No project name passed in event.")
        }

        def framework = frameworkService.rundeckFramework

        // get the jobUuid option
        def jobUuidOption = getAsyncImportStatusForProject(projectName).jobUuidOption

        // Options (false values bc we already imported the project with user's options in M1)
        def options = [
                jobUuidOption     :jobUuidOption,
                importExecutions  : true,
                importConfig      : false,
                importACL         : false,
                importScm         : false,
                validateJobref    : false,
                importNodesSources: false
        ] as ProjectArchiveParams

        try {
            asyncImportStatusFileUpdater(new AsyncImportStatusDTO(projectName, milestoneNumber).with {
                it.lastUpdate = "Iterating execution bundles."
                return it
            })

            // Distributed executions path
            def distributedExecutionsFullPath = Paths.get("${BASE_WORKING_DIR.toString()}${projectName}${File.separator}${DISTRIBUTED_EXECUTIONS_FILENAME}")
            // The first dir of distributed executions, in other words, the next execution bundle to be uploaded
            Path firstDir
            def executionBundles = null
            try {
                executionBundles = Files.walk(distributedExecutionsFullPath, FileVisitOption.FOLLOW_LINKS)
                        .filter(Files::isDirectory)
                        .filter(path -> path.getFileName().toString().matches("\\d+"))
                        .sorted(Comparator.comparingInt(path -> Integer.parseInt(path.getFileName().toString())))
                        .collect(Collectors.toList())

                if( executionBundles.size() ){

                    asyncImportStatusFileUpdater(new AsyncImportStatusDTO(projectName, milestoneNumber).with {
                        it.lastUpdate = "A total of ${executionBundles.size()} execution bundles found, iterating in progress.."
                        return it
                    })

                    executionBundles.forEach{ bundle ->
                        if (!Files.exists(bundle) || !Files.isDirectory(bundle)) {
                            throw new AsyncImportException("Bundle corrupted or not a directory.")
                        }
                        // Executions path
                        firstDir = bundle
                        // Model path
                        def modelProjectFullPath = Paths.get("${BASE_WORKING_DIR.toString()}${projectName}${File.separator}${MODEL_PROJECT_NAME_SUFFIX}")
                        // Executions path inside model
                        def modelProjectExecutionsContainerPath = Paths.get("${modelProjectFullPath}${File.separator}${MODEL_PROJECT_INTERNAL_PREFIX}${projectName}")
                        try {
                            // Move the first dir to model project
                            try {
                                Files.move(firstDir, modelProjectExecutionsContainerPath.resolve(EXECUTION_DIR_NAME), StandardCopyOption.REPLACE_EXISTING)
                            } catch (NoSuchFileException ignored) {
                                ignored.printStackTrace()
                                throw ignored
                            }

                            def zippedFilename = "${BASE_WORKING_DIR}${projectName}${File.separator}${firstDir.fileName}${MODEL_PROJECT_NAME_EXT}"
                            zipModelProject(modelProjectFullPath.toString(), zippedFilename);

                            FileInputStream fis = new FileInputStream(zippedFilename);

                            asyncImportStatusFileUpdater(new AsyncImportStatusDTO(projectName, milestoneNumber).with {
                                it.lastUpdate = "Uploading execution bundle #${firstDir.fileName}, ${executionBundles.size()} bundles remaining."
                                return it
                            })

                            def result

                            try {
                                result = projectService.importToProject(
                                        project,
                                        framework,
                                        authContext as UserAndRolesAuthContext,
                                        fis,
                                        options
                                )
                            } catch (Exception e) {
                                e.printStackTrace()
                            }

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
                                    asyncImportStatusFileUpdater(new AsyncImportStatusDTO(projectName, milestoneNumber).with {
                                        it.errors = "${e.stackTrace.toString()}"
                                        return it
                                    })
                                    throw e
                                }
                            }
                            if (result.execerrors) {
                                asyncImportStatusFileUpdater(new AsyncImportStatusDTO(projectName, milestoneNumber).with {
                                    it.errors = "${result.execerrors?.toString()}"
                                    return it
                                })
                            }
                        } catch (Exception e) {
                            asyncImportStatusFileUpdater(new AsyncImportStatusDTO(projectName, milestoneNumber).with {
                                it.errors = "${e.stackTrace.toString()}"
                                return it
                            })
                        }
                    }
                }

                deleteNonEmptyDir("${BASE_WORKING_DIR.toString()}${projectName}")

                asyncImportStatusFileUpdater(new AsyncImportStatusDTO(projectName, milestoneNumber).with {
                    it.milestone = AsyncImportMilestone.ASYNC_IMPORT_COMPLETED.name
                    it.lastUpdate = "All Executions uploaded, async import ended. Please check the target project."
                    return it
                })

            } catch (IOException e) {
                // Report the error
                asyncImportStatusFileUpdater(new AsyncImportStatusDTO(projectName, milestoneNumber).with {
                    it.errors = "${e.stackTrace.toString()}"
                    return it
                })
            }
        } catch (IOException e) {
            // Report the error
            asyncImportStatusFileUpdater(new AsyncImportStatusDTO(projectName, milestoneNumber).with {
                it.errors = "${e.stackTrace.toString()}"
                return it
            })
        }
    }

    @GrailsCompileStatic
    private static void zipModelProject(String unzippedFile, String zippedFile) throws IOException {
        FileOutputStream fos = new FileOutputStream(zippedFile);
        ZipOutputStream zos = new ZipOutputStream(fos);

        addDirToZip(new File(unzippedFile), "", zos);

        zos.close();
        fos.close();
    }

    @GrailsCompileStatic
    private static void addDirToZip(File dir, String relativePath, ZipOutputStream zos) throws IOException {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                addDirToZip(file, relativePath + file.getName() + File.separator, zos);
            } else {
                FileInputStream fis = new FileInputStream(file);
                ZipEntry zipEntry = new ZipEntry(relativePath + file.getName());
                zos.putNextEntry(zipEntry);

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    zos.write(buffer, 0, bytesRead);
                }
                fis.close();
                zos.closeEntry();
            }
        }
    }

    private static void deleteNonEmptyDir(String path){
        try {
            Files.walk(Paths.get(path))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createTempCopyFromStream(String destDir, InputStream inputStream){
        ZipInputStream zipInputStream = new ZipInputStream(inputStream)
        try {
            File checkDir = new File(destDir)
            if( !checkDir.exists() ){
                checkDir.mkdirs()
            }
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                String newFileName = zipEntry.getName();
                File destFile = new File(destDir, newFileName);

                if (zipEntry.isDirectory()) {
                    destFile.mkdirs();
                } else {
                    File parent = destFile.getParentFile();
                    if (!parent.exists()) {
                        parent.mkdirs();
                    }

                    FileOutputStream fos = new FileOutputStream(destFile);
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                    fos.close();
                }

                zipInputStream.closeEntry();
            }

            zipInputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
            e.printStackTrace()
        }
    }

    Path getInternalRundeckProjectPath(String path){
        return Files.list(Paths.get(path))
                .filter { it ->
                    it.fileName.toString().startsWith("rundeck-")
                }.collect(Collectors.toList())[0]
    }

    List<Path> getFilesPathsByPrefixAndExtensionInPath(String path, String prefix, String ext){
        try{
            return Files.list(Paths.get(path.toString()))
                    .sorted((s1, s2) -> {
                        int num1 = Integer.parseInt(s1.fileName.toString().replaceAll("\\D", ""));
                        int num2 = Integer.parseInt(s2.fileName.toString().replaceAll("\\D", ""));
                        return Integer.compare(num1, num2);
                    })
                    .filter {
                        it -> {
                            it.fileName.toString().startsWith(prefix) && it.fileName.toString().endsWith(ext)
                        }
                    }.collect(Collectors.toList())
        }catch(Exception e){
            e.printStackTrace()
            throw e
        }
    }

}
