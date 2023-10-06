package org.rundeck.util.container

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.shaded.com.google.common.collect.ImmutableList

import java.nio.file.Paths
import java.time.Duration

@CompileStatic
@Slf4j
class RdDockerContainer extends GenericContainer<RdDockerContainer> implements ClientProvider {

    private static final String STATIC_TOKEN = System.getenv("TEST_RUNDECK_CONTAINER_TOKEN") ?: 'letmein99'
    private static final String CONTEXT_PATH = System.getenv("TEST_RUNDECK_CONTAINER_CONTEXT") ?: '/rundeck'
    //Port bindings can be used in case of needing an static host port, EG: "0.0.0.0:8081:8080"
    private static final String PORT_BINDINGS = System.getenv("TEST_RUNDECK_PORT_BINDINGS")
    private static final Integer DEFAULT_PORT = System.getenv("TEST_RUNDECK_CONTAINER_PORT")?.toInteger() ?: 8080
    private static final String CUSTOM_ENVS = System.getenv("CUSTOM_ENVS")
    private static final String TOMCAT_TAG = System.getenv("TOMCAT_TAG") ?: "8.5.81-jdk11"

    RdDockerContainer(URI dockerFileLocation){
        super(new ImageFromDockerfile()
                        .withDockerfile(
                                Paths.get(dockerFileLocation)).withBuildArg("TOMCAT_TAG", TOMCAT_TAG))
        withExposedPorts(DEFAULT_PORT)
        setWaitStrategy(Wait.forHttp("${CONTEXT_PATH}/api/14/system/info")
                .forStatusCodeMatching(it -> it >= 200 && it < 500 && it != 404)
                .withStartupTimeout(Duration.ofMinutes(5)))
        if(PORT_BINDINGS != null && !PORT_BINDINGS.isEmpty()){
            setPortBindings(ImmutableList.of(PORT_BINDINGS))
        }
        if(CUSTOM_ENVS != null && !CUSTOM_ENVS.isEmpty()){
            CUSTOM_ENVS.split(',').each(){
                withEnv(it.split(":")[0], it.split(":")[1])
            }
        }
    }

    @Override
    void close() {
        super.close()
    }

    RdClient getClient() {
        clientWithToken(STATIC_TOKEN)
    }

    RdClient clientWithToken(String token) {
        RdClient.create("http://${host}:${firstMappedPort}${CONTEXT_PATH}", token)
    }
}
