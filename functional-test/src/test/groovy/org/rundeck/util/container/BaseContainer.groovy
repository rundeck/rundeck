package org.rundeck.util.container

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import spock.lang.Specification

import java.text.SimpleDateFormat
import java.util.function.Consumer
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

/**
 * Base class for tests, starts a shared static container for all tests
 */
@CompileStatic
@Slf4j
abstract class BaseContainer extends Specification implements ClientProvider {
    public static final String PROJECT_NAME = 'test'
    private static RdContainer RUNDECK
    private static final Object LOCK = new Object()
    private static ClientProvider CLIENT_PROVIDER
    private static final String DEFAULT_DOCKERFILE_LOCATION = System.getenv("DEFAULT_DOCKERFILE_LOCATION") ?: System.getProperty("DEFAULT_DOCKERFILE_LOCATION")

    ClientProvider getClientProvider() {
        if (System.getenv("TEST_RUNDECK_URL") != null) {
            if (CLIENT_PROVIDER == null) {
                CLIENT_PROVIDER = new ClientProvider() {
                    @Override
                    RdClient getClient() {
                        return RdClient.create(System.getenv("TEST_RUNDECK_URL"), System.getenv("TEST_RUNDECK_TOKEN"))
                    }

                    @Override
                    RdClient clientWithToken(String token) {
                        return RdClient.create(System.getenv("TEST_RUNDECK_URL"), token)
                    }
                }
            }
        } else if (DEFAULT_DOCKERFILE_LOCATION != null && !DEFAULT_DOCKERFILE_LOCATION.isEmpty() && CLIENT_PROVIDER == null){
            synchronized (LOCK) {
                RdDockerContainer rdDockerContainer = new RdDockerContainer(getClass().getClassLoader().getResource(DEFAULT_DOCKERFILE_LOCATION).toURI())
                rdDockerContainer.start()
                CLIENT_PROVIDER = rdDockerContainer
            }
        } else if (RUNDECK == null && DEFAULT_DOCKERFILE_LOCATION == null) {
            synchronized (LOCK) {
                log.info("Starting testcontainer: ${getClass().getClassLoader().getResource(System.getProperty("COMPOSE_PATH")).toURI()}")
                log.info("Starting testcontainer: RUNDECK_IMAGE: ${RdContainer.RUNDECK_IMAGE}")
                log.info("Starting testcontainer: LICENSE_LOCATION: ${RdContainer.LICENSE_LOCATION}")
                log.info("Starting testcontainer: TEST_RUNDECK_GRAILS_URL: ${RdContainer.TEST_RUNDECK_GRAILS_URL}")
                RUNDECK = new RdContainer(getClass().getClassLoader().getResource(System.getProperty("COMPOSE_PATH")).toURI())
                RUNDECK.start()
                CLIENT_PROVIDER = RUNDECK
            }
        }
        return CLIENT_PROVIDER
    }

    void setupProject() {
        setupProject(PROJECT_NAME)
    }

    void setupProject(String name) {
        def getProject = client.doGet("/project/${name}")
        if (getProject.code() == 404) {
            def post = client.doPost("/projects", [name: name])
            if (!post.successful) {
                throw new RuntimeException("Failed to create project: ${post.body().string()}")
            }
        }
    }

    /**
     * Build a url query string from a map of parameters
     * @param params
     * @return
     */
    static String buildUrlParams(Map params){
        return params.collect{
            "${it.key}=${it.value}"
        }.join("&")
    }
    /**
     * Setup a project with a project archive file resource path
     * @param name
     * @param archiveFileResourcePath
     */
    void setupProject(String name, String archiveFileResourcePath) {
        setupProject(
            name,
            archiveFileResourcePath,
            [
                importConfig: true,
                importACL: true,
                importNodesSources: true,
                importScm: true
            ]
        )
    }
    /**
     * Setup a project with a project archive file resource path
     * @param name
     * @param archiveFileResourcePath
     * @param params URL parameters for the import request
     */
    void setupProject(String name, String archiveFileResourcePath, Map params) {
        setupProjectArchiveFile(name,new File(getClass().getResource(archiveFileResourcePath).getPath()), params)
    }

    /**
     * Setup a project with a project archive directory resource path
     * @param name
     * @param projectArchiveDirectoryResourcePath
     */
    void setupProjectArchiveDirectoryResource(String name, String projectArchiveDirectoryResourcePath) {
        setupProjectArchiveDirectory(
            name,
            new File(getClass().getResource(projectArchiveDirectoryResourcePath).getPath())
        )
    }
    /**
     * Setup a project with a project archive directory
     * @param name
     * @param projectArchiveDirectory
     */
    void setupProjectArchiveDirectory(String name, File projectArchiveDirectory) {
        setupProjectArchiveDirectory(
            name,
            projectArchiveDirectory,
            [
                importConfig      : true,
                importACL         : true,
                importNodesSources: true
            ]
        )
    }
    /**
     * Setup a project with a project archive directory
     * @param name
     * @param projectArchiveDirectory
     * @param params URL parameters for the import request
     */
    void setupProjectArchiveDirectory(String name, File projectArchiveDirectory, Map params) {
        File tempFile = createArchiveJarFile(name, projectArchiveDirectory)
        setupProjectArchiveFile(name, tempFile, params)
        tempFile.delete()
    }

    /**
     * Create a temp file containing a rundeck project archive (jar) from the contents of a directory
     * @param name project name
     * @param projectArchiveDirectory directory containing the project files
     * @return
     */
    File createArchiveJarFile(String name, File projectArchiveDirectory) {
        if(!projectArchiveDirectory.isDirectory()){
            throw new IllegalArgumentException("Must be a directory")
        }
        //create a project archive from the contents of the directory
        def tempFile = File.createTempFile("import-temp-${name}", ".zip")
        tempFile.deleteOnExit()
        //create Manifest
        def manifest = new Manifest()
        manifest.mainAttributes.putValue("Manifest-Version", "1.0")
        manifest.mainAttributes.putValue("Rundeck-Archive-Project-Name", name)
        manifest.mainAttributes.putValue("Rundeck-Archive-Format-Version", "1.0")
        manifest.mainAttributes.putValue("Rundeck-Application-Version", "5.0.0")
        manifest.mainAttributes.putValue(
            "Rundeck-Archive-Export-Date",
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").format(new Date())
        )

        tempFile.withOutputStream { os ->
            def jos = new JarOutputStream(os, manifest)

            jos.withCloseable { jarOutputStream ->

                projectArchiveDirectory.eachFileRecurse { file ->
                    def entry = new JarEntry(projectArchiveDirectory.toPath().relativize(file.toPath()).toString())
                    jarOutputStream.putNextEntry(entry)
                    if (file.isFile()) {
                        file.withInputStream { is ->
                            jarOutputStream << is
                        }
                    }
                }
            }
        }
        tempFile
    }

    /**
     * Import a project with a project archive file
     * @param name
     * @param projectArchive the project archive file
     * @param params URL parameters for the import request
     */
    void setupProjectArchiveFile(String name, File projectArchive, Map params) {
        if(!projectArchive.isFile()){
            throw new IllegalArgumentException("Must be a file")
        }
        def getProject = client.doGet("/project/${name}")
        if (getProject.code() == 404) {
            def post = client.doPost("/projects", [name: name])
            if (!post.successful) {
                throw new RuntimeException("Failed to create project: ${post.body().string()}")
            }
            client.doPut("/project/${name}/import?${buildUrlParams(params)}", projectArchive)
        }else if(getProject.code() == 200){
            client.doPut("/project/${name}/import?${buildUrlParams(params)}", projectArchive)
        }
    }

    RdClient _client
    @Override
    RdClient getClient() {
        if (null == _client) {
            _client = createClient()
        }
        return _client
    }

    RdClient createClient() {
        return clientProvider.getClient()
    }
    Map<String, RdClient> tokenProviders = [:]

    @Override
    RdClient clientWithToken(final String token) {
        if (!tokenProviders.containsKey(token)) {
            tokenProviders[token] = clientProvider.clientWithToken(token)
        }
        return tokenProviders[token]
    }

    void startEnvironment() {
        getClientProvider()
    }

    //client helpers
    Response doGet(String path) {
        return client.doGet(path)
    }

    Response doDelete(String path) {
        return client.doDelete(path)
    }

    Response request(String path, Consumer<Request.Builder> consumer) {
        return client.request(path, consumer)
    }

    Map jsonValue(ResponseBody body) {
        jsonValue(body, Map)
    }

    <T> T jsonValue(ResponseBody body, Class<T> clazz) {
        return client.jsonValue(body, clazz)
    }

    <T> T get(String path, Class<T> clazz) {
        return client.get(path, clazz)
    }

    Response doPost(String path, Object body = null) {
        return client.doPost(path, body)
    }

    <T> T post(String path, Object body = null, Class<T> clazz) {
        return client.post(path, body, clazz)
    }

    /**
     * Executes a job identified by jobId and waits until the job execution is completed.
     *
     * @param jobId The identifier of the job to run.
     * @param body Additional parameters for the job execution. Default is null.
     * @return A Map containing the final execution details.
     */
    Map runJobAndWait(String jobId, Object body = null) {
        def path = "/job/${jobId}/run"
        def response = client.post(path, body, Map)
        def finalStatus = [
                'aborted',
                'failed',
                'succeeded',
                'timedout',
                'other'
        ]
        while(true) {
            def exec = client.get("/execution/${response.id}/output", Map)
            if (finalStatus.contains(exec.execState)) {
                return exec
            } else {
                sleep 10000
            }
        }
    }

    void deleteProject(String projectName) {
        def response = client.doDelete("/project/${projectName}")
        if (!response.successful) {
            throw new RuntimeException("Failed to delete project: ${response.body().string()}")
        }
    }

    def setupSpec() {
        startEnvironment()
    }
}