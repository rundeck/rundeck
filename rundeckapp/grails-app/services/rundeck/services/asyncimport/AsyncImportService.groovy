package rundeck.services.asyncimport

import com.dtolabs.rundeck.app.support.ProjectArchiveParams
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.util.ZipReader
import grails.compiler.GrailsCompileStatic
import grails.converters.JSON
import grails.events.EventPublisher
import grails.events.annotation.Subscriber
import groovy.json.JsonSlurper
import rundeck.services.FrameworkService
import rundeck.services.ProjectService

import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.FileAttribute
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

    // Constants
    static final Path BASE_WORKING_DIR = Paths.get(System.getProperty("user.home") + File.separator + "async-import-dirs")
    static final String TEMP_DIR = System.getProperty("java.io.tmpdir")
    static final String TEMP_PROJECT_SUFFIX = 'AImportTMP-'
    static final String JSON_FILE_PREFFIX = 'AImport-status-'
    static final String JSON_FILE_EXT = '.json'
    static final String EXECUTION_DIR_NAME = 'executions'
    static final String MODEL_PROJECT_NAME_SUFFIX = 'rundeck-model-project'
    static final String MODEL_PROJECT_NAME_EXT = '.jar'

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
            def statusFilepath = "${JSON_FILE_PREFFIX}${projectName}${JSON_FILE_EXT}"
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
    AsyncImportStatusDTO getAsyncImportStatusForProject(String projectName) {
        try{
            final def fwkProject = frameworkService.getFrameworkProject(projectName)
            ByteArrayOutputStream output = new ByteArrayOutputStream()
            fwkProject.loadFileResource(JSON_FILE_PREFFIX + projectName + JSON_FILE_EXT, output)
            def obj = new JsonSlurper().parseText(output.toString()) as AsyncImportStatusDTO
            log.debug("Object extracted: ${obj.toString()}")
            return obj
        }catch(Exception e){
            log.error("Error during the async import file extraction process: ${e.message}")
            throw e
        }
    }

    @Override
    Long updateAsyncImportStatus(AsyncImportStatusDTO updatedStatus) {
        try {
            return saveAsyncImportStatusForProject(null, updatedStatus)
        } catch (IOException e) {
            e.printStackTrace();
            throw e
        }
    }

    Long saveAsyncImportStatusForProject(String projectName = null, AsyncImportStatusDTO newStatus = null){
        def resource
        def statusPersist
        try {
            if( newStatus != null ){
                statusPersist = new AsyncImportStatusDTO(newStatus)
            }else{
                statusPersist = new AsyncImportStatusDTO()
                statusPersist.projectName = projectName
                statusPersist.lastUpdated = new Date().toString()
                statusPersist.lastUpdate = AsyncImportMilestone.M1_CREATED.name
            }
            def jsonStatus = statusPersist as JSON
            def inputStream = new ByteArrayInputStream(jsonStatus.toString().getBytes(StandardCharsets.UTF_8));
            if (!statusPersist.projectName || statusPersist.projectName.size() <= 0) {
                log.error("No project name provided in new status.")
                throw new MissingPropertyException("No project name provided in new status.")
            }
            final def fwkProject = frameworkService.getFrameworkProject(statusPersist.projectName)
            final def filename = JSON_FILE_PREFFIX + statusPersist.projectName + JSON_FILE_EXT
            resource = fwkProject.storeFileResource(filename, inputStream)
            inputStream.close();
            return resource
        } catch (IOException e) {
            e.printStackTrace();
            throw e
        }
    }

    def updateAsyncImportFileWithMilestoneAndLastUpdateForProject(String projectName, String milestone, String lastUpdate){
        try {
           if( !projectName ){
               log.error("No project name in async import event notification.")
           }
            def oldStatusFileContent = getAsyncImportStatusForProject(projectName)
            def newStatusFileContent = new AsyncImportStatusDTO(oldStatusFileContent)
                newStatusFileContent.milestone = milestone
                newStatusFileContent.lastUpdated = new Date().toString()
                newStatusFileContent.lastUpdate = lastUpdate
            saveAsyncImportStatusForProject(null, newStatusFileContent)
        } catch (Exception e) {
            e.printStackTrace();
            throw e
        }
    }

    def updateAsyncImportFileWithTempFilepathForProject(String projectName, String tempFilePath){
        try {
            if( !projectName ){
                log.error("No project name in async import event notification.")
            }
            def oldStatusFileContent = getAsyncImportStatusForProject(projectName)
            def newStatusFileContent = new AsyncImportStatusDTO(oldStatusFileContent)
            newStatusFileContent.lastUpdated = new Date().toString()
            newStatusFileContent.lastUpdate = "Updated temp filepath."
            newStatusFileContent.tempFilepath = tempFilePath
            saveAsyncImportStatusForProject(null, newStatusFileContent)
        } catch (Exception e) {
            e.printStackTrace();
            throw e
        }
    }

    def updateAsyncImportFileWithMilestoneAndLastUpdateAndErrorsForProject(
            String projectName,
            String milestone,
            String lastUpdate,
            String errors){
        try {
            if( !projectName ){
                log.error("No project name in async import event notification.")
            }
            def oldStatusFileContent = getAsyncImportStatusForProject(projectName)
            def newStatusFileContent = new AsyncImportStatusDTO(oldStatusFileContent)
            newStatusFileContent.milestone = milestone
            newStatusFileContent.lastUpdated = new Date().toString()
            newStatusFileContent.lastUpdate = lastUpdate
            newStatusFileContent.errors = errors
            saveAsyncImportStatusForProject(null, newStatusFileContent)
        } catch (IOException e) {
            e.printStackTrace();
            throw e
        }
    }

    @Subscriber(AsyncImportEvents.ASYNC_IMPORT_EVENT_MILESTONE_1)
    def beginMilestone1(
            final String projectName,
            AuthContext authContext,
            IRundeckProject project,
            InputStream inputStream
    ){
        //1. Copy the input stream in /tmp and if everything goes ok, report and call M2
        //2. Create the working dir
        //3. Create model_project

        //1-
        // a. Create destination dir
        String destDir = "${TEMP_DIR}/${TEMP_PROJECT_SUFFIX}${projectName}"
        try{
            createTempCopyOfStream(destDir, inputStream)
        }catch(Exception e){
            e.printStackTrace()
            throw e
        }
        // b. If all is ok, persist the temp path in the status file
        updateAsyncImportFileWithTempFilepathForProject(projectName, destDir)
        //2-
    }

    @Subscriber(AsyncImportEvents.ASYNC_IMPORT_EVENT_MILESTONE_3)
    def beginMilestone3(
            final String projectName,
            AuthContext authContext,
            IRundeckProject project
    ){

        updateAsyncImportFileWithMilestoneAndLastUpdateForProject(
                projectName,
                AsyncImportMilestone.M3_IMPORTING.name,
                "Milestone 3 in progress..."
        )

        def framework = frameworkService.rundeckFramework
        // Options
        def dummyOptions = [
                importExecutions  : true,
                importConfig      : false,
                importACL         : false,
                importScm         : false,
                validateJobref    : false,
                importNodesSources: false
        ] as ProjectArchiveParams

        try {
            if( !projectName ){
                throw new MissingPropertyException("No project name passed in event.")
            }
            // Distributed executions path
            def distributedExecutionsPath = "async-import-dirs${File.separator}distributed_executions"
            def distributedExecutionsFullPath = Paths.get(System.getProperty("user.home") + File.separator + distributedExecutionsPath)
            // The first dir of distributed executions, in other words, the next execution bundle to be uploaded
            Path firstDir
            List<Path> executionBundles = null
            try {
                executionBundles = Files.walk(distributedExecutionsFullPath, FileVisitOption.FOLLOW_LINKS)
                        .filter(Files::isDirectory)
                        .filter(path -> path.getFileName().toString().matches("\\d+"))
                        .sorted(Comparator.comparingInt(path -> Integer.parseInt(path.getFileName().toString())))
                        .collect(Collectors.toList());
                if (executionBundles.size() > 0) {
                    // Executions path
                    firstDir = executionBundles[0] as Path
                    // Model path
                    def modelProjectPath = "async-import-dirs${File.separator}model-project"
                    def modelProjectFullPath = Paths.get(System.getProperty("user.home") + File.separator + modelProjectPath)
                    // Executions path inside model
                    def modelProjectExecutionsContainerPath = "async-import-dirs${File.separator}model-project/rundeck-${projectName}"
                    def modelProjectExecutionsContainerFullPath = Paths.get(System.getProperty("user.home") + File.separator + modelProjectExecutionsContainerPath)
                    try {
                        // Move the first dir to model project
                        try{
                            Files.move(firstDir, modelProjectExecutionsContainerFullPath.resolve(EXECUTION_DIR_NAME), StandardCopyOption.REPLACE_EXISTING)
                        }catch(NoSuchFileException ignored){
                            ignored.printStackTrace()
                            throw ignored
                        }

                        def zippedFilename = "${BASE_WORKING_DIR}/${MODEL_PROJECT_NAME_SUFFIX}-${firstDir.fileName}${MODEL_PROJECT_NAME_EXT}"
                        zipModelProject(modelProjectFullPath as String, zippedFilename);

                        FileInputStream fis = new FileInputStream(zippedFilename);

                        updateAsyncImportFileWithMilestoneAndLastUpdateForProject(
                                projectName,
                                AsyncImportMilestone.M3_IMPORTING.name,
                                "Uploading execution bundle #${firstDir.fileName} of #XXX."
                        )

                        def result = projectService.importToProject(
                                project,
                                framework,
                                authContext as UserAndRolesAuthContext,
                                fis,
                                dummyOptions
                        )

                        if( result.success ){
                            def executionsDirPath = "${modelProjectExecutionsContainerFullPath.toString()}${File.separator}executions"
                            try{
                                if( Files.exists(Paths.get(executionsDirPath)) && Files.isDirectory(Paths.get(executionsDirPath)) ){
                                    deleteNonEmptyDir(executionsDirPath)
                                }else{
                                    throw new FileNotFoundException("Executions directory don't exist or is not a directory.")
                                }
                                if( Files.exists(Paths.get(zippedFilename)) ){
                                    Files.delete(Paths.get(zippedFilename))
                                }else{
                                    throw new FileNotFoundException("Zipped model project not found.")
                                }
                            }catch(Exception e){
                                throw e
                            }
                        }

                        if( result.execerrors ){
                            Throwable t = new Exception(result.execerrors.toString())
                            reportError(
                                    projectName,
                                    AsyncImportMilestone.M3_IMPORTING.name,
                                    "Error in Milestone 3.",
                                    t
                            )
                            throw t
                        }

                        // Update the status file and emit M3 event
                        projectService.beginAsyncImportMilestone3(
                                projectName,
                                authContext,
                                project
                        )

                    } catch (Exception e) {
                        reportError(
                                projectName,
                                AsyncImportMilestone.M3_IMPORTING.name,
                                "Error in Milestone 3.",
                                e
                        )
                    }
                }else{
                    // Remove all the files in the working dir and that's it!!
                    deleteNonEmptyDir(BASE_WORKING_DIR.toString())
                    // Update the file
                    updateAsyncImportFileWithMilestoneAndLastUpdateForProject(
                            projectName,
                            AsyncImportMilestone.ASYNC_IMPORT_COMPLETED.name,
                            "All Executions uploaded, async import ended. Please check the target project."
                    )
                }
            } catch (IOException e) {
                // Report the error
                reportError(
                        projectName,
                        AsyncImportMilestone.M3_IMPORTING.name,
                        "Error in Milestone 3.",
                        e
                )
            }
        } catch (IOException e) {
            // Report the error
            reportError(
                    projectName,
                    AsyncImportMilestone.M3_IMPORTING.name,
                    "Error in Milestone 3.",
                    e
            )
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
                addDirToZip(file, relativePath + file.getName() + "/", zos);
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

    private void reportError(String projectName, String milestone, String updateMessage, Exception errors){
        updateAsyncImportFileWithMilestoneAndLastUpdateAndErrorsForProject(
                projectName,
                milestone,
                updateMessage,
                getStacktraceAsString(errors)
        )
    }

    private static String getStacktraceAsString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    private static void createTempCopyOfStream(String destDir, InputStream inputStream){
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
}
