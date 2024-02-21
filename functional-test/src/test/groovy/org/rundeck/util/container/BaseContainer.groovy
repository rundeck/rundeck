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

    void setupProject(String name, String projectImportLocation) {
        def getProject = client.doGet("/project/${name}")
        if (getProject.code() == 404) {
            def post = client.doPost("/projects", [name: name])
            if (!post.successful) {
                throw new RuntimeException("Failed to create project: ${post.body().string()}")
            }
            client.doPut("/project/${name}/import?importConfig=true&importACL=true&importNodesSources=true", new File(getClass().getResource(projectImportLocation).getPath()))
        }else if(getProject.code() == 200){
            client.doPut("/project/${name}/import?importConfig=true&importACL=true&importNodesSources=true", new File(getClass().getResource(projectImportLocation).getPath()))
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

    Integer runJob(String jobId, Object body = null) {
        def path = "/job/${jobId}/run"
        def response = client.post(path, body, Map)
        response.id as Integer
    }

    Map obtainExecution(int executionId) {
        def finalStatus = [
                'aborted',
                'failed',
                'succeeded',
                'timedout',
                'other'
        ]
        while(true) {
            def exec = client.get("/execution/${executionId}/output", Map)
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

    def updateFile(String fileName, String projectName = null, String jobName = null, String groupName = null, String description = null, String args = null, String args2 = null, String uuid = null) {
        def pathXmlFile = getClass().getResource("/test-files/${fileName}").getPath()
        def xmlProjectContent = new File(pathXmlFile).text
        def xmlProject = xmlProjectContent
                .replaceAll('xml-uuid', uuid?:UUID.randomUUID().toString())
                .replaceAll('xml-project-name', projectName?:PROJECT_NAME)
                .replaceAll('xml-args', args?:"echo hello there")
                .replaceAll('xml-2-args', args2?:"echo hello there 2")
                .replaceAll('xml-job-name', jobName?:'job-test')
                .replaceAll('xml-job-group-name', groupName?:'group-test')
                .replaceAll('xml-job-description-name', description?:'description-test')
        def tempFile = File.createTempFile("temp", ".xml")
        tempFile.text = xmlProject
        tempFile.deleteOnExit()
        tempFile.path
    }

    def jobImportFile(String projectName = null, String pathXmlFile) {
        def responseImport = client.doPost("/project/${projectName?:PROJECT_NAME}/jobs/import", new File(pathXmlFile), "application/xml")
        responseImport.successful
        responseImport.code() == 200
        client.jsonValue(responseImport.body(), Map)
    }

}