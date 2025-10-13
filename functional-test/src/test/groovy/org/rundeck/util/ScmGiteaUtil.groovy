package org.rundeck.util

import org.slf4j.Logger
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait

import java.time.Duration

class ScmGiteaUtil {
    static void composeContainer(final ComposeContainer composeContainer, Logger log) {
        composeContainer
            .withLogConsumer("gitea", new Slf4jLogConsumer(log))
            .waitingFor(
                "gitea",
                Wait.forHttp("/api/healthz")
                    .forStatusCode(200)
                    .withStartupTimeout(
                        Duration.ofMinutes(5)
                    )
            )
    }
}
