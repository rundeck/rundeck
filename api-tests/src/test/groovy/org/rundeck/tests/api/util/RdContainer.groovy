package org.rundeck.tests.api.util

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.MountableFile

/**
 * Rundeck server via testcontainers
 */
@CompileStatic
@Slf4j
class RdContainer extends GenericContainer<RdContainer> implements ClientProvider {

    private static final Integer DEFAULT_PORT = 4440
    //matches tokens.properties in src/main/resources
    public static final String STATIC_TOKEN = 'admintoken'


    RdContainer(String dockerImageName) {
        super(dockerImageName);
        withExposedPorts(DEFAULT_PORT)
        withCopyFileToContainer(
            MountableFile.forClasspathResource(
                "realm.properties"
            ),
            "/home/rundeck/server/config/realm.properties"
        )
        withCopyFileToContainer(
            MountableFile.forClasspathResource(
                "tokens.properties"
            ),
            "/home/rundeck/server/config/tokens.properties"
        )
        withEnv("RUNDECK_TOKENS_FILE", "/home/rundeck/server/config/tokens.properties")
        withEnv("RUNDECK_GRAILS_URL", "http://localhost:4440")
        withEnv("RUNDECK_MULTIURL_ENABLED", "true")
        withLogConsumer(new Slf4jLogConsumer(log))
        waitingFor(
            Wait.forHttp("/api/14/system/info")
                .forStatusCodeMatching(it -> it >= 200 && it < 500)
        )
    }


    RdClient getClient() {
        clientWithToken(STATIC_TOKEN)
    }

    RdClient clientWithToken(String token) {
        RdClient.create("http://${host}:${firstMappedPort}", token)
    }

    @Override
    void close() {
        super.close()
    }
}
