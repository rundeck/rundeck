package org.rundeck.tests.functional.selenium

import org.rundeck.tests.functional.selenium.pages.JobCreatePage
import org.rundeck.tests.functional.selenium.pages.JobListPage
import org.rundeck.tests.functional.selenium.pages.HomePage
import org.rundeck.tests.functional.selenium.pages.JobTab
import org.rundeck.tests.functional.selenium.pages.LoginPage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase

@SeleniumCoreTest
class JobsSpec extends SeleniumBase {

    def setupSpec(){
        setupProject("SeleniumBasic", "/projects-import/SeleniumBasic.zip")
    }

    def "edit job set description"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            def homePage = page HomePage
            homePage.goProjectHome"SeleniumBasic"
        when:
            def jobListPage = page JobListPage
            jobListPage.loadPathToEditJob "SeleniumBasic", "b7b68386-3a52-46dc-a28b-1a4bf6ed87de"
            jobListPage.go()
        then:
            def jobCreatePage = page JobCreatePage
            jobCreatePage.descriptionTextarea.clear()
            jobCreatePage.descriptionTextarea.sendKeys 'a new job description'
            jobCreatePage.updateJobButton.click()
        expect:
            'a new job description' == jobCreatePage.descriptionTextLabel.getText()
    }

    def "edit job set groups"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            def homePage = page HomePage
            homePage.goProjectHome"SeleniumBasic"
        when:
            def jobListPage = page JobListPage
            jobListPage.loadPathToEditJob "SeleniumBasic", "b7b68386-3a52-46dc-a28b-1a4bf6ed87de"
            jobListPage.go()
        then:
            def jobCreatePage = page JobCreatePage
            jobCreatePage.jobGroupField.clear()
            jobCreatePage.jobGroupField.sendKeys 'testGroup'
            jobCreatePage.updateJobButton.click()
        expect:
        'testGroup' == jobCreatePage.jobInfoGroupLabel.getText()
    }

    def "edit job and set schedules tab"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            def homePage = page HomePage
            homePage.goProjectHome"SeleniumBasic"
        when:
            def jobListPage = page JobListPage
            jobListPage.loadPathToEditJob "SeleniumBasic", "b7b68386-3a52-46dc-a28b-1a4bf6ed87de"
            jobListPage.go()
        then:
            def jobCreatePage = page JobCreatePage
            jobCreatePage.tab JobTab.SCHEDULE click()
            jobCreatePage.scheduleRunYesField.click()
            if (!jobCreatePage.scheduleEveryDayCheckboxField.isSelected()) {
                jobCreatePage.scheduleEveryDayCheckboxField.click()
            }
        expect:
            jobCreatePage.scheduleDaysCheckboxDivField.isDisplayed()
    }

    def "edit job and set other tab"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            def homePage = page HomePage
            homePage.goProjectHome"SeleniumBasic"
        when:
            def jobListPage = page JobListPage
            jobListPage.loadPathToEditJob "SeleniumBasic", "b7b68386-3a52-46dc-a28b-1a4bf6ed87de"
            jobListPage.go()
        then:
            def jobCreatePage = page JobCreatePage
            jobCreatePage.tab JobTab.OTHER click()
            if (jobCreatePage.multiExecFalseField.isSelected()) {
                jobCreatePage.multiExecTrueField.click()
                jobCreatePage.multiExecTrueField.isSelected()
            } else {
                jobCreatePage.multiExecFalseField.click()
                jobCreatePage.multiExecFalseField.isSelected()
            }
    }

}
