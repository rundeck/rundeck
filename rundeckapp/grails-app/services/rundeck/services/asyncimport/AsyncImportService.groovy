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
import org.rundeck.app.components.project.ProjectComponent
import rundeck.services.FrameworkService
import rundeck.services.ProjectService

import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitOption
import java.nio.file.Files
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
            }
            // For reporting
            def oldFileStatusContentAsObject = getAsyncImportStatusForProject(projectName)

            def baseWorkDirPath = Paths.get(System.getProperty("user.home") + File.separator + "async-import-dirs")
            def distributedExecutionsPath = "async-import-dirs${File.separator}distributed_executions"
            def distributedExecutionsFullPath = Paths.get(System.getProperty("user.home") + File.separator + distributedExecutionsPath)
            Path firstDir //First dir, for the model
            // First dir extraction
            try {
                List<Path> executionBundles = Files.walk(distributedExecutionsFullPath, FileVisitOption.FOLLOW_LINKS)
                        .filter(Files::isDirectory)
                        .filter(path -> path.getFileName().toString().matches("\\d+"))
                        .sorted(Comparator.comparingInt(path -> Integer.parseInt(path.getFileName().toString())))
                        .collect(Collectors.toList());
                if (!executionBundles.isEmpty()) { // Cambiar por un While(haya carpetas)
                    // Executions path
                    firstDir = executionBundles[0] as Path
                    // Model path
                    def modelProjectPath = "async-import-dirs${File.separator}model-project"
                    def modelProjectFullPath = Paths.get(System.getProperty("user.home") + File.separator + modelProjectPath)
                    // Executions path inside model
                    def modelProjectExecutionsContainerPath = "async-import-dirs${File.separator}model-project/rundeck-${projectName}"
                    def modelProjectExecutionsContainerFullPath = Paths.get(System.getProperty("user.home") + File.separator + modelProjectExecutionsContainerPath)
                    // Move the first dir to model project
                    try {
                        if( firstDir ){
                            Files.move(firstDir, modelProjectExecutionsContainerFullPath.resolve(EXECUTION_DIR_NAME), StandardCopyOption.REPLACE_EXISTING)
                        }else{
                            // do something
                        }
                        println("File moved!")
                        def zippedFilename = "${baseWorkDirPath}/${MODEL_PROJECT_NAME_SUFFIX}-${firstDir.fileName}${MODEL_PROJECT_NAME_EXT}"
                        zipModelProject(modelProjectFullPath as String, zippedFilename);

                        // Make an InputStream from the zip file
                        FileInputStream fis = new FileInputStream(zippedFilename);

                        // delete the "executions from model" after upload
                        def result = projectService.importToProject(
                                project,
                                framework,
                                authContext as UserAndRolesAuthContext,
                                fis,
                                dummyOptions
                        )
                        def hi = "hello"
                    } catch (IOException e) {
                        println("Error al mover el directorio: ${e.message}")
                        throw e
                    }
                } else {
                    // *****PROCESS ENDED************
                    println("El directorio está vacío o no contiene carpetas.")
                }
            } catch (IOException e) {
                println("Exception while reading or sorting the distributed executions list.")
            }
        } catch (IOException e) {
            e.printStackTrace();
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
}
