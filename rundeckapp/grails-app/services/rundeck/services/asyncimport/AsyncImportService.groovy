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
import org.yaml.snakeyaml.introspector.MissingProperty
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
import java.util.zip.ZipOutputStream

class AsyncImportService implements AsyncImportStatusFileOperations, EventPublisher {

    FrameworkService frameworkService
    ProjectService projectService

    // Constants
    static final String JSON_FILE_PREFFIX = 'AImport-status-'
    static final String JSON_FILE_EXT = '.json'
    static final String EXECUTION_DIR_NAME = 'executions'
    static final String MODEL_PROJECT_NAME_SUFFIX = 'rundeck-model-project'
    static final String MODEL_PROJECT_NAME_EXT = '.jar'

    @Override
    Long createStatusFile(String projectName) {
        try {
            saveAsyncImportStatusForProject(projectName, null)
        } catch (IOException e) {
            e.printStackTrace();
            throw e
        }
    }

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
        }
        return null
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

    @Subscriber(AsyncImportEvents.ASYNC_IMPORT_EVENT_TEST_UPDATE)
    def updateAsyncImportFileLastUpdateForProject(String projectName, String lastUpdate){
        try {
           if( !projectName ){
               log.error("No project name in async import event notification.")
           }
            def oldStatusFileContent = getAsyncImportStatusForProject(projectName)
            def newStatusFileContent = new AsyncImportStatusDTO(oldStatusFileContent)
                newStatusFileContent.lastUpdated = new Date().toString()
                newStatusFileContent.lastUpdate = lastUpdate
            saveAsyncImportStatusForProject(null, newStatusFileContent)
        } catch (IOException e) {
            e.printStackTrace();
            throw e
        }
    }

    @Subscriber(AsyncImportEvents.ASYNC_IMPORT_EVENT_MILESTONE_3)
    def beginMilestone3(
            final String projectName,
            AuthContext authContext,
            IRundeckProject project
    ){

        def framework = frameworkService.rundeckFramework
        def dummyOptions = [
                importExecutions  : true,
                importConfig      : false,
                importACL         : false,
                importScm         : false,
                validateJobref    : false,
                importNodesSources: false
        ] as ProjectArchiveParams

        // Options
        try {
            if( !projectName ){
                log.error("No project name in async import event notification.")
                throw new MissingPropertyException("No project name passed in event.")
            }
            // For reporting
            def oldFileStatusContentAsObject = getAsyncImportStatusForProject(projectName)

            def baseWorkDirPath = Paths.get(System.getProperty("user.home") + File.separator + "async-import-dirs")
            def distributedExecutionsPath = "async-import-dirs${File.separator}distributed_executions"
            def distributedExecutionsFullPath = Paths.get(System.getProperty("user.home") + File.separator + distributedExecutionsPath)
            Path firstDir //First dir, for the model
            // First dir extraction
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

                        def zippedFilename = "${baseWorkDirPath}/${MODEL_PROJECT_NAME_SUFFIX}-${firstDir.fileName}${MODEL_PROJECT_NAME_EXT}"
                        zipModelProject(modelProjectFullPath as String, zippedFilename);

                        FileInputStream fis = new FileInputStream(zippedFilename);

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
                                log.error("Exception while deleting files:" + e.stackTrace)
                                throw e
                            }
                        }

                        // Update the status file and emit M3 event
                        projectService.beginAsyncImportMilestone3(
                                projectName,
                                authContext,
                                project
                        )

                    } catch (Exception e) {
                        e.printStackTrace()
                        throw e
                    }
                }else{
                    // Update the file

                    // Remove all the files in the working dir and that's it!!
                    deleteNonEmptyDir(baseWorkDirPath.toString())
                }
            } catch (IOException e) {
                e.printStackTrace()
                throw e
            }
        } catch (IOException e) {
            e.printStackTrace()
            throw e
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
}
