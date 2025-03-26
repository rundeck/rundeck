package org.rundeck.util.container

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.rundeck.util.api.responses.execution.Execution
import org.rundeck.util.api.storage.KeyStorageApiClient
import org.rundeck.util.common.WaitBehaviour
import org.rundeck.util.common.WaitUtils
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.jobs.JobUtils
import spock.lang.Specification

import java.text.SimpleDateFormat
import java.time.Duration
import java.util.function.Consumer
import java.util.function.Function
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

import org.slf4j.LoggerFactory;

/**
 * Base class for tests, starts a shared static container for all tests
 */
@Slf4j
abstract class BaseContainer extends Specification implements ClientProvider, WaitBehaviour {

    public static final String PROJECT_NAME = 'test'
    private static final Object CLIENT_PROVIDER_LOCK = new Object()
    private static ClientProvider CLIENT_PROVIDER
    private static final String DEFAULT_DOCKERFILE_LOCATION = System.getenv("DEFAULT_DOCKERFILE_LOCATION") ?: System.getProperty("DEFAULT_DOCKERFILE_LOCATION")
    protected static final String TEST_RUNDECK_URL = System.getenv("TEST_RUNDECK_URL") ?: System.getProperty("TEST_RUNDECK_URL")
    protected static final String TEST_RUNDECK_TOKEN = System.getenv("TEST_RUNDECK_TOKEN") ?: System.getProperty("TEST_RUNDECK_TOKEN", "admintoken")
    protected static final ObjectMapper MAPPER = new ObjectMapper()
    private static String RUNDECK_CONTAINER_ID

    String getCustomDockerComposeLocation(){
        return null
    }

    ClientProvider getClientProvider() {
        synchronized (CLIENT_PROVIDER_LOCK) {

            if (CLIENT_PROVIDER != null) {
                return CLIENT_PROVIDER
            }

            if (TEST_RUNDECK_URL != null) {
                CLIENT_PROVIDER = new ClientProvider() {
                    @Override
                    RdClient getClient() {
                        return RdClient.create(TEST_RUNDECK_URL, TEST_RUNDECK_TOKEN)
                    }

                    @Override
                    RdClient clientWithToken(String token) {
                        return RdClient.create(TEST_RUNDECK_URL, token)
                    }
                }

            } else if (DEFAULT_DOCKERFILE_LOCATION != null && !DEFAULT_DOCKERFILE_LOCATION.isBlank()) {
                RdDockerContainer rdDockerContainer = new RdDockerContainer(getClass().getClassLoader().getResource(DEFAULT_DOCKERFILE_LOCATION).toURI())
                rdDockerContainer.start()
                CLIENT_PROVIDER = rdDockerContainer
                RUNDECK_CONTAINER_ID = rdDockerContainer.containerId

            } else if (getCustomDockerComposeLocation() != null && !getCustomDockerComposeLocation().isBlank()) {
                // Override default timeout values to accommodate slow container startups
                Map<String, Integer> clientConfig = Map.of(
                        "readTimeout", 60,
                )
                String featureName = System.getProperty("TEST_FEATURE_ENABLED_NAME")

                log.info("Starting testcontainer: ${getClass().getClassLoader().getResource(getCustomDockerComposeLocation()).toURI()}")
                log.info("Starting testcontainer: RUNDECK_IMAGE: ${RdComposeContainer.RUNDECK_IMAGE}")
                log.info("Starting testcontainer: LICENSE_LOCATION: ${RdComposeContainer.LICENSE_LOCATION}")
                log.info("Starting testcontainer: TEST_RUNDECK_GRAILS_URL: ${RdComposeContainer.TEST_RUNDECK_GRAILS_URL}")
                var rundeckComposeContainer = new RdComposeContainer(
                        getClass().getClassLoader().getResource(getCustomDockerComposeLocation()).toURI(),
                        featureName,
                        clientConfig
                )
                rundeckComposeContainer.start()
                CLIENT_PROVIDER = rundeckComposeContainer
                RUNDECK_CONTAINER_ID = rundeckComposeContainer.getRundeckContainerId()

            } else {
                // Override default timeout values to accommodate slow container startups
                Map<String, Integer> clientConfig = Map.of(
                    "readTimeout", 60,
                )

                String featureName = System.getProperty("TEST_FEATURE_ENABLED_NAME")
                log.info("Starting testcontainer: ${getClass().getClassLoader().getResource(System.getProperty("COMPOSE_PATH")).toURI()}")
                log.info("Starting testcontainer: RUNDECK_IMAGE: ${RdComposeContainer.RUNDECK_IMAGE}")
                log.info("Starting testcontainer: LICENSE_LOCATION: ${RdComposeContainer.LICENSE_LOCATION}")
                log.info("Starting testcontainer: TEST_RUNDECK_GRAILS_URL: ${RdComposeContainer.TEST_RUNDECK_GRAILS_URL}")
                var rundeckComposeContainer = new RdComposeContainer(
                    getClass().getClassLoader().getResource(System.getProperty("COMPOSE_PATH")).toURI(),
                    featureName,
                    clientConfig
                )
                rundeckComposeContainer.start()
                CLIENT_PROVIDER = rundeckComposeContainer
                RUNDECK_CONTAINER_ID = rundeckComposeContainer.getRundeckContainerId()
            }

            return CLIENT_PROVIDER
        }
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
        } else if (!getProject.successful) {
            throw new RuntimeException("Failed to access project: ${getProject.body().string()}")
        }
    }

    /**
     * Import system ACL file
     * @param resourcePathName resource path to the file to upload
     * @param aclPath acl path within the system
     */
    void importSystemAcls(String resourcePathName, String aclPath){
        def getAcl = client.doGet("/system/acl/${aclPath}")
        def aclFile = new File(getClass().getResource(resourcePathName).getPath())
        def resp
        if (getAcl.code() == 404) {
            //POST
            resp = client.doPost("/system/acl/${aclPath}", aclFile, 'application/yaml')
        }else{
            //PUT
            resp = client.doPut("/system/acl/${aclPath}", aclFile, 'application/yaml')
        }
        if (!resp.successful) {
            throw new RuntimeException("Failed to create System ACL: ${resp.body().string()}")
        }
    }

    /**
     * Import system ACL file
     * @param resourcePathName resource path to the file to upload
     * @param aclPath acl path within the system
     */
    void deleteSystemAcl(String aclPath){
        def resp = client.doDelete("/system/acl/${aclPath}")
        if (!(resp.code() in [204, 404])) {
            throw new RuntimeException("Failed to delete System ACL: ${resp.body().string()}")
        }
    }

    /**
     * Build a url query string from a map of parameters
     * @param params
     * @return
     */
    static String buildUrlParams(Map params) {
        return params.collect {
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
                importConfig      : true,
                importACL         : true,
                importNodesSources: true,
                importScm         : true
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
        setupProjectArchiveFile(name, new File(getClass().getResource(archiveFileResourcePath).getPath()), params)
    }

    def loadKeysForNodes(String baseKeyPath, String project, String nodeKeyPassPhrase, String nodeUserPassword, String userVaultPassword) {
        client.doPost("/storage/keys/project/$project/ssh-node.key", new File("${baseKeyPath}/id_rsa"), "application/octet-stream")
        client.doPost("/storage/keys/project/$project/ssh-node-passphrase.key", new File("${baseKeyPath}/id_rsa_passphrase"), "application/octet-stream")
        if (nodeKeyPassPhrase) loadKey("project/$project/ssh-node-passphrase.pass", nodeKeyPassPhrase, "password")
        if (nodeUserPassword) loadKey("project/$project/ssh-node.pass", nodeUserPassword, "password")
        if (userVaultPassword) loadKey("project/$project/vault-user.pass", userVaultPassword, "password")

    }

    def loadKey(String path, String dbPass, String keyType) {
        KeyStorageApiClient keyStorageApiClient = new KeyStorageApiClient(clientProvider)
        keyStorageApiClient.callUploadKey(path, keyType, dbPass)
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
                importNodesSources: true,
                importScm         : true
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
     * Retrieves the execution output for the specified execution ID as a list of strings. One line per element.
     * @param execId The identifier of the execution to retrieve the output from.
     * @return A list of strings, each representing a line of the execution output.
     */
    List<String> getExecutionOutputLines(String execId) {
        def execOutput = JobUtils.getExecutionOutput(execId, client)
        return execOutput.entries.collect { it.log }
    }

    /**
     * Create a temp file containing a rundeck project archive (jar) from the contents of a directory
     * @param name project name
     * @param projectArchiveDirectory directory containing the project files
     * @return
     */
    File createArchiveJarFile(String name, File projectArchiveDirectory) {
        if (!projectArchiveDirectory.isDirectory()) {
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
        if (!projectArchive.isFile()) {
            throw new IllegalArgumentException("Must be a file")
        }
        try (Response getProject = client.doGet("/project/${name}")) {
            if (getProject.code() == 404) {
                try (def post = client.doPost("/projects", [name: name])) {
                    if (!post.successful) {
                        throw new RuntimeException("Failed to create project: ${post.body().string()}")
                    }
                    try (def put = client.doPut("/project/${name}/import?${buildUrlParams(params)}", projectArchive)) {
                        if (!put.successful) {
                            throw new RuntimeException("Failed to upload archive: ${put.body().string()}")
                        }
                    }

                }
            } else if (getProject.code() == 200) {
                try (def put = client.doPut("/project/${name}/import?${buildUrlParams(params)}", projectArchive)) {
                    if (!put.successful) {
                        throw new RuntimeException("Failed to upload archive: ${put.body().string()}")
                    }
                }
            } else if(!getProject.successful){
                throw new RuntimeException("Failed to access project: ${getProject.body().string()}")
            }
        }
    }

    def waitingResourceEnabled(String project, String nodename) {
        def client = clientProvider.client
        def response = client.doGet("/project/$project/resources")
        Map<String, Map> nodeList = safelyMap(response, Map.class, { [:] })
        println(nodeList)
        def count = 0

        waitFor(
                {
                    //force refresh project
                    client.doPutWithJsonBody("/project/$project/config/time", ["time": System.currentTimeMillis()])
                    response = client.doGet("/project/$project/resources")
                    safelyMap(response, Map.class, { [:] })
                },
                { it.get(nodename) != null },
                WaitingTime.EXCESSIVE
        )
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
     * Runs a job and wait for it to finish. Returning the output of the execution.
     *
     * @param jobId The job UUID to run.
     * @param body An object representing the job run request options. Must be serializable to JSON.
     * @return The output of the execution.
     * @deprecated Use JobUtils.executeJobWithOptions(),
     * JobUtils.waitForExecutionFinish() instead, and JobUtils.getExecutionOutput() instead.
     */
    @Deprecated
    Map runJobAndWait(String jobId, Object body = null) {
        def r = JobUtils.executeJobWithOptions(jobId, client, body)
        if (!r.successful) {
            throw new RuntimeException("Failed to run job: ${r}")
        }

        Execution execution = MAPPER.readValue(r.body().string(), Execution.class)
        waitForExecutionFinish(execution.id as String, WaitingTime.EXCESSIVE)

        // Maintains the data contract for the Map return type
        return client.get("/execution/${execution.id}/output", Map)
    }

    /**
     * Waits for the execution with the specified ID to reach one of the common terminal statuses.
     * @param executionId The identifier of the execution to monitor.
     * @param timeout wait timeout
     * @return
     */
    Execution waitForExecutionFinish(String executionId, Duration timeout = WaitingTime.MODERATE) {
        final List<String> statusesToWaitFor = [
                'aborted',
                'failed',
                'succeeded',
                'timedout',
                'other']

        return JobUtils.waitForExecution(statusesToWaitFor, executionId, client, timeout)
    }

    /**
     * Deletes the specified project.
     * This method sends a DELETE request to remove the project with the given name.
     * If the deletion operation fails, a RuntimeException is thrown.
     *
     * @param projectName the name of the project to be deleted. Must not be null.
     * @throws RuntimeException if the project deletion fails.
     *         The exception contains a detailed message obtained from the server's response.
     */
    void deleteProject(String projectName) {
        def response = client.doDelete("/project/${projectName}")
        if (!response.successful) {
            throw new RuntimeException("Failed to delete project: ${response.body().string()}")
        }
    }

    /**
     * Updates the configuration of a project with the provided settings.
     *
     * This method sends a PUT request to update the configuration of the specified project
     * with the provided settings. The configuration data is replaced entirely with the submitted values.
     *
     * @param projectName The name of the project whose configuration is to be updated. Must not be null.
     * @param body A map containing the configuration settings to be applied to the project.
     *             The content of this map should represent the entire configuration data to replace.
     *             The structure of the map should match the expected format for the project configuration.
     *             Must not be null.
     * @throws RuntimeException if updating the project configuration fails.
     *         The exception contains a detailed message obtained from the server's response.
     */
    void updateConfigurationProject(String projectName, Map body) {
        def responseDisable = client.doPutWithJsonBody("/project/${projectName}/config", body)
        if (!responseDisable.successful) {
            throw new RuntimeException("Failed to disable scheduled execution: ${responseDisable.body().string()}")
        }
    }

    def setupSpec() {
        startEnvironment()
    }

    /**
     * Pauses the execution for a specified number of seconds.
     * This method utilizes the sleep function to pause the current thread for the given duration.
     * If the thread is interrupted while sleeping, it catches the InterruptedException and logs the error.
     *
     *  Usage note: Waiting unconditionally is considered to be an anti-pattern in tests. Avoid using unless necessary. See the deprecation notice for more information.
     *
     * @param seconds the number of seconds to pause the execution. This value should be positive.
     * @throws IllegalArgumentException if the `seconds` parameter is negative, as `Duration.ofSeconds` cannot process negative values.
     * @deprecated in favor of WaitBehaviour.waitFor())
     */
    void hold(int seconds) {
        try {
            sleep Duration.ofSeconds(seconds).toMillis()
        } catch (InterruptedException e) {
            log.error("Interrupted", e)
        }
    }

    /**
     * Adds additional configuration options to a project.
     * @param project The name of the project to add configuration
     * @param configure configuration map
     */
    def addExtraProjectConfig(String project, Map<String, String> configure) {
        configure.forEach { key, value ->
            Response response = client.doPutWithJsonBody("/project/$project/config/$key",
                ["key": key, "value": value]
            )
            if (!response.successful) {
                throw new RuntimeException("Failed to add configuration options to a project: ${response.body().string()}")
            }
        }
    }

    /**
     * Maps successful  responses to the specified type.
     * Successful responses are the ones that fall into the [200..300) range.
     *  Unsuccessful responses are passed to the unsuccessfulResponseHandler.
     * @param response
     * @param valueType
     * @param unsuccessfulResponseHandler a closure that receives a response object to handle. If omitted, returns a null.
     * @return
     */
    static <T> T safelyMap(Response response, Class<T> valueType, Closure<T> unsuccessfulResponseHandler = { null }) {
        response.successful ? MAPPER.readValue(response.body().string(), valueType) :  unsuccessfulResponseHandler(response)
    }


    void executeDockerActionOnRundeckContainer(String action) {
        def process = "docker ${action} ${RUNDECK_CONTAINER_ID}".execute()
        def stdout = new StringWriter()
        def stderr = new StringWriter()
        process.consumeProcessOutput(stdout, stderr)

        def code = process.waitFor()

        if (code != 0) {
            throw new RuntimeException("Failed to ${action} container ${RUNDECK_CONTAINER_ID} with code ${code} - ${stderr.toString()}")
        }
    }



    void waitForRundeckAppToBeResponsive(){

        def checkIsRundeckApiResponding = {
            try {
                def response = client.doGet("/system/info")
                return (response != null && response.code() == 200)
            } catch (Exception e) {
                LoggerFactory.getLogger(BaseContainer.class).info(e.getMessage())
                return false
            }
        }

        WaitUtils.waitFor(
                checkIsRundeckApiResponding,
                {
                    it
                },
                WaitingTime.XTRA_EXCESSIVE,
                WaitingTime.LOW

        )
    }

    void restartRundeckContainer() {
        executeDockerActionOnRundeckContainer("stop")
        executeDockerActionOnRundeckContainer("start")
        waitForRundeckAppToBeResponsive()
    }

    List<String> getAllLogs(String execId, Function<Map, String> paramGen, int max=20) {
        def logs = []
        def logging = [
                lastmod: "0",
                offset : "0",
                done   : false,
        ]
        def count = 1
        while (!logging.done && count < max) {
            def params = paramGen.apply(logging)
            Map output = get("/execution/${execId}/output?${params}", Map)
            assert output != null
            assert output.id == execId
            if (output.entries && output.entries.size() > 0) {
                logs.addAll(output.entries*.log)
            }
            if (output.offset != null) {
                logging.offset = output.offset
            }
            if (output.lastModified) {
                logging.lastmod = output.lastModified
            }
            if (output.completed != null && output.execCompleted != null) {
                logging.done = output.execCompleted && output.completed
            }
            if (output.unmodified == true) {
                sleep(2000)
            } else if (!logging.done) {
                sleep(1000)
            }
            count++
        }
        return logs
    }

}
