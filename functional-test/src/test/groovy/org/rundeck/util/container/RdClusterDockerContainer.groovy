package org.rundeck.util.container

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.shaded.com.google.common.collect.ImmutableList

import java.nio.file.Paths
import java.time.Duration

@CompileStatic
@Slf4j
class RdClusterDockerContainer extends DockerComposeContainer<RdClusterDockerContainer> implements ClientProvider {

    public static final String DEFAULT_SERVICES_TO_EXPOSE = System.getenv("TEST_RUNDECK_CONTAINERS_SERVICE") ?: 'rundeck-1'
    private static final String STATIC_TOKEN = System.getenv("TEST_RUNDECK_CONTAINER_TOKEN") ?: 'admintoken'
    private static final Integer DEFAULT_PORT = System.getenv("TEST_RUNDECK_CONTAINER_PORT")?.toInteger() ?: 4440
    public static final String RUNDECK_IMAGE = System.getenv("TEST_IMAGE") ?: System.getProperty("TEST_IMAGE")
    public static final String LICENSE_LOCATION = System.getenv("LICENSE_LOCATION")

    RdClusterDockerContainer(URI dockerFileLocation) {
        super(new File(dockerFileLocation))
        withExposedService(DEFAULT_SERVICES_TO_EXPOSE, DEFAULT_PORT,
                Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(10)))
        withEnv("TEST_IMAGE", RUNDECK_IMAGE)
        withEnv("LICENSE_LOCATION", LICENSE_LOCATION)
        withLogConsumer(DEFAULT_SERVICES_TO_EXPOSE, new Slf4jLogConsumer(log))
        withRemoveVolumes(true)
        withRemoveImages(RemoveImages.ALL)
        waitingFor(DEFAULT_SERVICES_TO_EXPOSE,
                Wait.forLogMessage(".*Grails application running.*", 1)
                        .withStartupTimeout(Duration.ofMinutes(10)))
    }

    @Override
    void close() {
        super.close()
    }

    RdClient getClient() {
        clientWithToken(STATIC_TOKEN)
    }

    RdClient clientWithToken(String token) {
        RdClient.create("http://${getServiceHost(DEFAULT_SERVICES_TO_EXPOSE, DEFAULT_PORT)}:${getServicePort(DEFAULT_SERVICES_TO_EXPOSE, DEFAULT_PORT)}", token)
    }
}
