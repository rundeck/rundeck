package org.rundeck.util.container

import com.fasterxml.jackson.databind.ObjectMapper
import com.jcraft.jsch.JSch
import com.jcraft.jsch.KeyPair
import groovy.util.logging.Slf4j
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.rundeck.util.api.responses.execution.Execution
import org.rundeck.util.api.responses.execution.ExecutionOutput
import org.rundeck.util.api.storage.KeyStorageApiClient
import org.rundeck.util.common.WaitBehaviour
import org.rundeck.util.common.WaitUtils
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.jobs.JobUtils
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import org.testcontainers.containers.ComposeContainer
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.function.Consumer
import java.util.function.Function
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.logging.Level
import java.util.logging.Logger

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
    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger()

        SLF4JBridgeHandler.install()
        //fine logging for http client closeable leaks
        Logger.getLogger(OkHttpClient.name).setLevel(Level.FINEST)
    }

    @Shared
    @AutoCleanup
    Set<Closeable> toClose = new ClosingSet<>()
    static class ClosingSet<T> extends HashSet<T> implements Closeable {
        @Override
        void close() throws IOException {
            if(size()>0){
                System.err.println("AutoClosing ${size()} responses")
            }
            this.each {
                if(it instanceof Closeable){
                    try{
                        (it as Closeable).close()
                    } catch (IOException e){
                        //ignore
                    }
                }
            }
        }
    }


    String getCustomDockerComposeLocation(){
        return null
    }

    ClientProvider getClientProvider() {
        //fine logging for http client closeable leaks
        Logger.getLogger(OkHttpClient.name).setLevel(Level.FINEST)
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

            } else {
                URI resource
                if (getCustomDockerComposeLocation() != null && !getCustomDockerComposeLocation().isBlank()) {
                    resource = getClass().getClassLoader().getResource(getCustomDockerComposeLocation()).toURI()
                } else {
                    resource = getClass().getClassLoader().getResource(System.getProperty("COMPOSE_PATH")).toURI()
                }

                // Override default timeout values to accommodate slow container startups
                Map<String, Integer> clientConfig = Map.of(
                        "readTimeout", 60,
                )
                String featureName = System.getProperty("TEST_FEATURE_ENABLED_NAME")

                log.info("Starting testcontainer: ${resource}")
                log.info("Starting testcontainer: RUNDECK_IMAGE: ${RdComposeContainer.RUNDECK_IMAGE}")
                log.info("Starting testcontainer: LICENSE_LOCATION: ${RdComposeContainer.LICENSE_LOCATION}")
                log.info("Starting testcontainer: TEST_RUNDECK_GRAILS_URL: ${RdComposeContainer.TEST_RUNDECK_GRAILS_URL}")
                var rundeckComposeContainer = new RdComposeContainer(
                    resource,
                    featureName,
                    clientConfig
                )
                composeContainer(rundeckComposeContainer)
                rundeckComposeContainer.start()
                CLIENT_PROVIDER = rundeckComposeContainer
                RUNDECK_CONTAINER_ID = rundeckComposeContainer.getRundeckContainerId()

            }

            return CLIENT_PROVIDER
        }
    }

    /**
     * Customize the compose container if needed, will be called after creation but before start
     * @param composeContainer
     */
    void composeContainer(ComposeContainer composeContainer){

    }


    void setupProject() {
        setupProject(PROJECT_NAME)
    }

    void setupProject(String name, Map config = [:]) {
        try (def getProject = client.doGet("/project/${name}")) {
            if (getProject.code() == 404) {
                def result = client.post("/projects", config + [name: name])
            } else if (!getProject.successful) {
                throw new RuntimeException("Failed to access project: ${getProject.body().string()}")
            }
        }
    }

    /**
     * Import system ACL file
     * @param resourcePathName resource path to the file to upload
     * @param aclPath acl path within the system
     */
    void importSystemAcls(String resourcePathName, String aclPath){
        try(def getAcl = client.doGet("/system/acl/${aclPath}")) {
            def aclFile = new File(getClass().getResource(resourcePathName).getPath())
            try (
                def resp = getAcl.code() == 404 ?
                           client.doPost("/system/acl/${aclPath}", aclFile, 'application/yaml') :
                           client.doPut("/system/acl/${aclPath}", aclFile, 'application/yaml')
            ) {

                if (!resp.successful) {
                    throw new RuntimeException("Failed to create System ACL: ${resp.body().string()}")
                }
            }
        }
    }

    /**
     * Import system ACL file
     * @param resourcePathName resource path to the file to upload
     * @param aclPath acl path within the system
     */
    void deleteSystemAcl(String aclPath){
        try(def resp = client.doDelete("/system/acl/${aclPath}")) {
            if (!(resp.code() in [204, 404])) {
                throw new RuntimeException("Failed to delete System ACL: ${resp.body().string()}")
            }
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
        loadKeyFile("project/$project/ssh-node.key", new File("${baseKeyPath}/id_rsa"), "privateKey")
        loadKeyFile("project/$project/ssh-node-passphrase.key", new File("${baseKeyPath}/id_rsa_passphrase"), "privateKey")
        if (nodeKeyPassPhrase) loadKey("project/$project/ssh-node-passphrase.pass", nodeKeyPassPhrase, "password")
        if (nodeUserPassword) loadKey("project/$project/ssh-node.pass", nodeUserPassword, "password")
        if (userVaultPassword) loadKey("project/$project/vault-user.pass", userVaultPassword, "password")

    }

    def loadKey(String path, String dbPass, String keyType) {
        KeyStorageApiClient keyStorageApiClient = new KeyStorageApiClient(clientProvider)
        keyStorageApiClient.callUploadKey(path, keyType, dbPass)
    }
    def loadKeyFile(String path, File file, String keyType) {
        KeyStorageApiClient keyStorageApiClient = new KeyStorageApiClient(clientProvider)
        keyStorageApiClient.callUploadKeyFile(path, keyType, file)
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

        //force refresh project
        Map res = client.putWithJsonBody(
            "/project/$project/config/time",
            ["value": System.currentTimeMillis().toString()]
        )
        assert res.value != null

        waitFor(
                {
                    response = client.doGet("/project/$project/resources")
                    safelyMap(response, Map.class, {  [:] })
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

    /**
     * Save a response to be closed later
     * @param response
     * @return
     */
    private Response closeLater(Response response) {
        if(response == null){
            return null
        }
        toClose << response
        return response
    }

    //client helpers
    Response doGet(String path) {
        closeLater(client.doGet(path))
    }

    Response doDelete(String path) {
        closeLater client.doDelete(path)
    }

    Response doRequest(String path, Consumer<Request.Builder> consumer) {
        closeLater client.doRequest(path, consumer)
    }

    <T> T request(String path, Class<T> clazz = Map, Consumer<Request.Builder> consumer) {
        def response = client.doRequest(path, consumer)
        if(!response.successful){
            throw new RuntimeException("Request failed: ${response.body().string()}")
        }
        closeLater response
        jsonValue(response.body(), clazz)
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
        closeLater client.doPost(path, body)
    }

    <T> T post(String path, Object body = null, Class<T> clazz) {
        return client.post(path, body, clazz)
    }

    /**
     * encode url parameters and return the parameter string without the ?
     * @param params
     * @return
     */
    static String urlParams(Map<String,Object> params){
        return params.collect{ k,v -> "${URLEncoder.encode(k,'UTF-8')}=${URLEncoder.encode(v.toString(),'UTF-8')}"}.join("&")
    }

    /**
     * Runs a job and wait for it to finish. Returning the output of the execution.
     *
     * @param jobId The job UUID to run.
     * @param body An object representing the job run request options. Must be serializable to JSON.
     * @return The output of the execution.
     * @deprecated Use {@link #runJobGetOutput(java.lang.String)} or JobUtils.executeJobWithOptions(),
     * JobUtils.waitForExecutionFinish() instead, and JobUtils.getExecutionOutput() instead.
     */
    @Deprecated
    Map runJobAndWait(String jobId, Object body = null) {
        Execution execution
        try (def r = JobUtils.executeJobWithOptions(jobId, client, body)) {
            if (!r.successful) {
                throw new RuntimeException("Failed to run job: ${r}")
            }

            execution = jsonValue(r.body(), Execution.class)
        }
        waitForExecutionFinish(execution.id as String, WaitingTime.EXCESSIVE)

        // Maintains the data contract for the Map return type
        return client.get("/execution/${execution.id}/output", Map)
    }

    /**
     * Runs a job and wait for it to finish. Returning the execution and output.
     *
     * @param jobId The job UUID to run.
     * @param body An object representing the job run request options. Must be serializable to JSON.
     * @return A wrapper containing the final execution and the output of the execution.
     */
    RunJobOutput runJobGetOutput(String jobId, Object body = null) {
        Execution execution
        try (def r = JobUtils.executeJobWithOptions(jobId, client, body)) {
            if (!r.successful) {
                throw new RuntimeException("Failed to run job: ${r}")
            }

            execution = jsonValue(r.body(), Execution.class)
        }
        execution = waitForExecutionFinish(execution.id as String, WaitingTime.EXCESSIVE)
        def output=JobUtils.getExecutionOutput(execution.id, client)
        return new RunJobOutput(execution: execution, output: output)
    }
    static class RunJobOutput{
        Execution execution
        ExecutionOutput output
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
     * The operation is idempotent - if the project does not exist, the method returns
     * successfully without throwing an exception.
     *
     * @param projectName the name of the project to be deleted. Must not be null.
     * @throws RuntimeException if the project deletion fails for reasons other than the project not existing.
     *         The exception contains a detailed message obtained from the server's response.
     */
    void deleteProject(String projectName) {
        def response = closeLater client.doDelete("/project/${projectName}")
        
        int statusCode = response.code()
        ResponseBody responseBody = response.body()
        String responseBodyString = null
        try {
            if (responseBody != null) {
                responseBodyString = responseBody.string()
            }
        } catch (IOException ignored) {
            // If we can't read the body, we'll fall back to a generic error message later.
        }
        
        // Successful delete returns 204 (No Content)
        if (statusCode == 204) {
            return
        }
        
        // If project doesn't exist (404), treat as success (idempotent operation)
        if (statusCode == 404 && responseBodyString) {
            try {
                Map errorBody = MAPPER.readValue(responseBodyString, Map)
                if (errorBody?.errorCode == "api.error.project.missing") {
                    // Project already deleted or doesn't exist - this is fine
                    return
                }
                // If 404 but different error code, fall through to throw exception
            } catch (Exception ignored) {
                // If we can't parse the error, fall through to throw exception
            }
        }
        
        // For any other error (or 404 with different error code), throw exception
        String errorMessage = responseBodyString ?: "HTTP ${statusCode}: ${response.message()}"
        throw new RuntimeException("Failed to delete project: ${errorMessage}")
    }

    /**
     * Deletes the specified project key.
     * This method sends a DELETE request to remove the project key with the given name.
     * If the deletion operation fails, a RuntimeException is thrown.
     *
     * @param projectName the name of the key's parent project to be deleted. Must not be null.
     * @param keyPath the path of the project key to be deleted. Must not be null.
     * @throws RuntimeException if the project deletion fails.
     *         The exception contains a detailed message obtained from the server's response.
     */
    void deleteProjectKey(String projectName, String keyPath) {
        def response = closeLater client.doDelete("/storage/keys/project/${projectName}/${keyPath}")
        if (!response.successful) {
            throw new RuntimeException("Failed to delete project key: ${response.body().string()}")
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
        def result = client.putWithJsonBody("/project/${projectName}/config", body)
    }

    def setupSpec() {
        setupPrivateSSHKeys()
        startEnvironment()
    }

    void setupPrivateSSHKeys() {
        def tempKeyDir = ".build/tmp/keys"

        def ossResource = getClass().getClassLoader().getResource("docker/compose/oss")
        def proResource = getClass().getClassLoader().getResource("docker/compose/pro")

        def ossKeyPath = ossResource ? ossResource.getPath() + "/keys" : null
        def proKeyPath = proResource ? proResource.getPath() + "/keys" : null

        generatePrivateKey(tempKeyDir, "id_rsa")
        generatePrivateKey(tempKeyDir, "id_rsa_passphrase", "testpassphrase123")
        copyKeyToDestinations(tempKeyDir, "id_rsa", [ossKeyPath, proKeyPath].findAll { it })
        copyKeyToDestinations(tempKeyDir, "id_rsa_passphrase", [ossKeyPath, proKeyPath].findAll { it })
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
            Map result = client.putWithJsonBody("/project/$project/config/$key",
                ["key": key, "value": value]
            )
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
    <T> T safelyMap(Response response, Class<T> valueType, Closure<T> unsuccessfulResponseHandler = { null }) {
        response.successful ? MAPPER.readValue(response.body().string(), valueType) :  unsuccessfulResponseHandler(closeLater(response))
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
            //use httpClient directly to invoke a non-api path
            try (def response =
                client.httpClient.newCall(
                    new Request.Builder().
                        url("${client.baseUrl}/actuator/health/readiness").
                        header('Accept', 'application/json').
                        get().
                        build()
                ).execute()){
                return response.successful
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
                WaitingTime.ABSURDLY_EXCESSIVE,
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

    static def generatePrivateKey(String tempDirPath, String keyName, String passphrase = null){
        File dir = new File(tempDirPath)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        File privateKeyFile = new File(tempDirPath + File.separator + keyName)
        File publicKeyFile = new File(tempDirPath + File.separator + keyName + ".pub")

        if (privateKeyFile.exists() && publicKeyFile.exists()) {
            return
        }

        JSch jsch = new JSch()
        KeyPair keyPair = KeyPair.genKeyPair(jsch, KeyPair.RSA)

        if (passphrase) {
            keyPair.writePrivateKey(privateKeyFile.absolutePath, passphrase.getBytes())
        } else {
            keyPair.writePrivateKey(privateKeyFile.absolutePath)
        }

        keyPair.writePublicKey(publicKeyFile.absolutePath, "test private key")
        keyPair.dispose()

        Set<PosixFilePermission> perms = new HashSet<>()
        perms.add(PosixFilePermission.OWNER_READ)
        perms.add(PosixFilePermission.OWNER_WRITE)
        Files.setPosixFilePermissions(privateKeyFile.toPath(), perms)
    }

    static def copyKeyToDestinations(String tempDirPath, String keyName, List<String> destinationPaths) {
        File privateKeyFile = new File(tempDirPath + File.separator + keyName)
        File publicKeyFile = new File(tempDirPath + File.separator + keyName + ".pub")

        destinationPaths.each { destPath ->
            File destDir = new File(destPath)
            if (!destDir.exists()) {
                destDir.mkdirs()
            }

            Files.copy(privateKeyFile.toPath(), new File(destDir, keyName).toPath(), StandardCopyOption.REPLACE_EXISTING)
            Files.copy(publicKeyFile.toPath(), new File(destDir, keyName + ".pub").toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
    /**
     * Generate a unique project name given the input name, replaces invalid chars and truncates to 20 chars, appends a timestamp
     * @param name
     * @return
     */
    static String generateProjectName(String name = "test") {
        return "${name.replaceAll(/[^a-zA-Z0-9_-]+/, '_').substring(0, 20)}-${System.currentTimeMillis()}"
    }

}
