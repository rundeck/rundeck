package org.rundeck.util.container

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.MountableFile

import java.time.Duration

@CompileStatic
@Slf4j
class RdContainer extends DockerComposeContainer<RdContainer> implements ClientProvider {

    public static final String DEFAULT_SERVICE_TO_EXPOSE = System.getProperty("TEST_RUNDECK_CONTAINER_SERVICE") ?: 'rundeck'
    private static final Integer DEFAULT_PORT = System.getProperty("TEST_RUNDECK_CONTAINER_PORT")?.toInteger() ?: 4440
    private static final String CONTEXT_PATH = System.getProperty("TEST_RUNDECK_CONTAINER_CONTEXT") ?: ''
    //matches tokens.properties in src/main/resources
    public static final String STATIC_TOKEN = System.getProperty("TEST_RUNDECK_CONTAINER_TOKEN") ?: 'admintoken'
    public static final String RUNDECK_IMAGE = System.getenv("TEST_RUNDECK_IMAGE") ?: System.getProperty("TEST_RUNDECK_IMAGE")


    RdContainer(URI composeFilePath) {

        super(new File(composeFilePath))
        if (CONTEXT_PATH && !CONTEXT_PATH.startsWith('/')) {
            throw new IllegalArgumentException("Context path must start with /")
        }
        withExposedService(DEFAULT_SERVICE_TO_EXPOSE, DEFAULT_PORT)
        withEnv("TEST_RUNDECK_IMAGE", RUNDECK_IMAGE)
        withEnv("TEST_RUNDECK_URL", System.getProperty("TEST_RUNDECK_URL"))
        withLogConsumer(DEFAULT_SERVICE_TO_EXPOSE, new Slf4jLogConsumer(log))
        waitingFor(DEFAULT_SERVICE_TO_EXPOSE,
                Wait.forHttp("${CONTEXT_PATH}/api/14/system/info")
                        .forStatusCodeMatching(it -> it >= 200 && it < 500 && it != 404)
                        .withStartupTimeout(Duration.ofMinutes(5))
        )
    }


    RdClient getClient() {
        clientWithToken(STATIC_TOKEN)
    }

    RdClient clientWithToken(String token) {
        RdClient.create("http://${getServiceHost(DEFAULT_SERVICE_TO_EXPOSE,DEFAULT_PORT)}:${getServicePort(DEFAULT_SERVICE_TO_EXPOSE,DEFAULT_PORT)}${CONTEXT_PATH}", token)
    }

    @Override
    void close() {
        super.close()
    }
}