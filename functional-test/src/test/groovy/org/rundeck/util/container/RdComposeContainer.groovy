package org.rundeck.util.container

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.containers.ContainerState
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait

import java.time.Duration

@CompileStatic
@Slf4j
class RdComposeContainer extends ComposeContainer implements ClientProvider {

    public static final String DEFAULT_SERVICE_TO_EXPOSE = System.getenv("TEST_RUNDECK_CONTAINER_SERVICE") ?: 'rundeck'
    private static final Integer DEFAULT_PORT = System.getenv("TEST_RUNDECK_CONTAINER_PORT")?.toInteger() ?: 4440
    private static final String CONTEXT_PATH = System.getenv("TEST_RUNDECK_CONTAINER_CONTEXT") ?: ''
    //matches tokens.properties in src/main/resources
    public static final String STATIC_TOKEN = System.getenv("TEST_RUNDECK_CONTAINER_TOKEN") ?: 'admintoken'
    public static final String RUNDECK_IMAGE = System.getenv("TEST_IMAGE") ?: System.getProperty("TEST_IMAGE")
    public static final String LICENSE_LOCATION = System.getenv("LICENSE_LOCATION")
    public static final String TEST_RUNDECK_GRAILS_URL = System.getenv("TEST_RUNDECK_GRAILS_URL") ?: "http://localhost:4440"
    public static final String TEST_TARGET_PLATFORM = System.getenv("TEST_TARGET_PLATFORM") ?: "linux/amd64"
    public static final boolean USE_LOCAL_DOCKER_COMPOSE = (System.getenv("USE_LOCAL_DOCKER_COMPOSE") ?: "true").toBoolean()

    private final Map<String, Integer> clientConfig

    /**
     *
     * @param composeFilePath
     * @param featureName an optional feature name to enable
     */
    RdComposeContainer(URI composeFilePath, String featureName, Map<String, Integer> clientConfig = Collections.emptyMap()) {
        super(new File(composeFilePath))
        this.clientConfig = clientConfig
        if (CONTEXT_PATH && !CONTEXT_PATH.startsWith('/')) {
            throw new IllegalArgumentException("Context path must start with /")
        }
        withLocalCompose(USE_LOCAL_DOCKER_COMPOSE)
        withExposedService(DEFAULT_SERVICE_TO_EXPOSE, DEFAULT_PORT, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(600)))
        withEnv("TEST_IMAGE", RUNDECK_IMAGE)
        withEnv("LICENSE_LOCATION", LICENSE_LOCATION)
        withEnv("TEST_RUNDECK_GRAILS_URL", TEST_RUNDECK_GRAILS_URL)
        withEnv("TEST_TARGET_PLATFORM", TEST_TARGET_PLATFORM)
        withEnv("TEST_RUNDECK_FEATURE_NAME", featureName ?: 'placeholderFeatureName')
        withLogConsumer(DEFAULT_SERVICE_TO_EXPOSE, new Slf4jLogConsumer(log))
        waitingFor(DEFAULT_SERVICE_TO_EXPOSE,
            Wait.forHttp("${CONTEXT_PATH}/api/14/system/info")
                .forPort(DEFAULT_PORT)
                .forStatusCodeMatching(it -> it >= 200 && it < 500 && it != 404)
                .withStartupTimeout(Duration.ofMinutes(10))
        )

    }

    String getRundeckContainerId(){
        Optional<ContainerState> containerState = getContainerByServiceName("rundeck")
        return containerState.isPresent() ? containerState.get().containerId : null
    }


    RdClient getClient() {
        clientWithToken(STATIC_TOKEN)
    }

    RdClient clientWithToken(String token) {
        RdClient.create(TEST_RUNDECK_GRAILS_URL, token, clientConfig)
    }

    @Override
    void close() {
        super.close()
    }
}