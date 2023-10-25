package org.rundeck.tests.functional.selenium

import org.rundeck.tests.functional.selenium.pages.JobListPage
import org.rundeck.tests.functional.selenium.pages.HomePage
import org.rundeck.tests.functional.selenium.pages.JobPage
import org.rundeck.tests.functional.selenium.pages.LoginPage
import org.rundeck.tests.functional.selenium.pages.ProjectCreatePage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.setup.NavLinkTypes

//@SeleniumCoreTest
class JobsSpec extends SeleniumBase {

    public static final String TEST_USER = System.getenv("RUNDECK_TEST_USER") ?: "admin"
    public static final String TEST_PASS = System.getenv("RUNDECK_TEST_PASS") ?: "admin"

    def "edit job and set groups"() {
        when:
            def description = 'demo description'
            def loginPage = page LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            def homePage = page HomePage
            homePage.createProject()
        then:
            def projectCreatePage = page ProjectCreatePage
            projectCreatePage.createProject(toCamelCase specificationContext.currentFeature.name)
            projectCreatePage.goTo NavLinkTypes.JOBS
            sleep 5000
            def jobListPage = page JobListPage
            jobListPage.createJobButton.click()
            def jobPage = page JobPage
            jobPage.createSimpleJob "simpleJob", null
            jobPage.actionField.click()
            jobPage.editJobField.click()
            sleep 5000
            jobPage.descriptionField.sendKeys description
            jobPage.waitForElementVisible jobPage.updateJob
            jobPage.updateJobButton.click()
            description == jobPage.descriptionTextLabel.getText()
        cleanup:
            projectCreatePage.deleteProject()
            projectCreatePage.waitForModal 1
            projectCreatePage.validatePage()
    }

}
