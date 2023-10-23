package org.rundeck.tests.functional.selenium

import org.openqa.selenium.By
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.setup.BaseSpec
import org.rundeck.util.setup.NavLinkTypes

//@SeleniumCoreTest
class JobsSpec extends BaseSpec {

    def "edit job and set groups"() {
        when:
            doLogin()
            createProject(toCamelCase(specificationContext.currentFeature.name), null)
            intoProjectGoTo(NavLinkTypes.JOBS)
        then:
            createSimpleJob('simpleJob', 'echo ${job.id}')
            driver.findElement(By.linkText("Action"))
            driver.findElement(By.linkText("Edit this Job"))
        cleanup:
            deleteProject(true)
    }

}
