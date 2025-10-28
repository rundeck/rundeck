package org.rundeck.tests.functional.api.scm

import groovy.util.logging.Slf4j
import org.rundeck.util.ScmGiteaUtil
import org.rundeck.util.container.BaseContainer
import org.testcontainers.containers.ComposeContainer

@Slf4j
class ScmBaseContainer extends BaseContainer {

    @Override
    void composeContainer(final ComposeContainer composeContainer) {
        ScmGiteaUtil.composeContainer(composeContainer, log)
    }
}
