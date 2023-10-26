package org.rundeck.tests.functional.selenium

import org.rundeck.tests.functional.selenium.pages.JobCreatePage
import org.rundeck.tests.functional.selenium.pages.JobListPage
import org.rundeck.tests.functional.selenium.pages.HomePage
import org.rundeck.tests.functional.selenium.pages.LoginPage
import org.rundeck.tests.functional.selenium.pages.ProjectCreatePage
import org.rundeck.tests.functional.selenium.pages.SideBarPage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.setup.NavLinkTypes

@SeleniumCoreTest
class JobsSpec extends SeleniumBase {

    def "edit job set description and groups"() { //TODO: Edit job set description and groups
        when:
            //def description = 'demo description'
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            def homePage = page HomePage
            homePage.createProjectButton()
        then:
            def projectCreatePage = page ProjectCreatePage
            projectCreatePage.createProject(toCamelCase specificationContext.currentFeature.name)
            def sideBarPage = page SideBarPage
            sideBarPage.goTo NavLinkTypes.JOBS
            sleep 5000
            def jobListPage = page JobListPage
            jobListPage.createJobButton.click()
            def jobPage = page JobCreatePage
            jobPage.createSimpleJob "simpleJob", null
            jobPage.createButton.click()
            sleep 5000
            //Update
            jobPage.actionField.click()
            jobPage.editJobField.click()
            //jobPage.descriptionField.sendKeys description
            jobPage.waitForElementVisible jobPage.updateJobButton
            jobPage.updateJobButton.click()
            //description == jobPage.descriptionTextLabel.getText()
        cleanup:
            sideBarPage.deleteProject()
            sideBarPage.waitForModal 1
    }

}
