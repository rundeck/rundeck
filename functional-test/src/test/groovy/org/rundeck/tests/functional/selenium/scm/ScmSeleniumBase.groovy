package org.rundeck.tests.functional.selenium.scm

import groovy.util.logging.Slf4j
import org.rundeck.util.ScmGiteaUtil
import org.rundeck.util.container.SeleniumBase
import org.testcontainers.containers.ComposeContainer

@Slf4j
class ScmSeleniumBase extends SeleniumBase {

    @Override
    void composeContainer(final ComposeContainer composeContainer) {
        ScmGiteaUtil.composeContainer(composeContainer, log)
    }
}
