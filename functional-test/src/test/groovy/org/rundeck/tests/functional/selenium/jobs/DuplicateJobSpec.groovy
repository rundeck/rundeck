package org.rundeck.tests.functional.selenium.jobs

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.login.LoginPage

@SeleniumCoreTest
class DuplicateJobSpec extends SeleniumBase{

    def setup() {
        go(LoginPage).login(TEST_USER, TEST_PASS)
    }

    def "duplicate job"(){
        given:
        String projectName = "duplicateJob"
        setupProject(projectName)
        String jobUuid = JobUtils.jobImportFile(projectName, '/test-files/test.xml', client).succeeded.first().id
        JobShowPage jobShowPage = page(JobShowPage, projectName).forJob(jobUuid)
        JobCreatePage jobCreatePage = page JobCreatePage
        when:
        jobShowPage.go()
        jobShowPage.getJobActionDropdownButton().click()
        jobShowPage.getDuplicateJobButton().click()
        then:
        jobCreatePage.validateCopyPage()
        when:
        jobCreatePage.saveJob()
        jobShowPage.setLoadPath("/job/show")
        then:
        jobShowPage.validatePage()
        !jobCreatePage.currentUrl().contains(jobUuid)
        cleanup:
        deleteProject(projectName)
    }

    def "duplicate job to project"() {
        given:
        String projectName = "duplicateJobOriginalProject"
        String duplicatedProjectName = "duplicatedProject"
        setupProject(projectName)
        setupProject(duplicatedProjectName)
        String jobUuid = JobUtils.jobImportFile(projectName, '/test-files/test.xml', client).succeeded.first().id
        JobShowPage jobShowPage = page(JobShowPage, projectName).forJob(jobUuid)
        JobCreatePage jobCreatePage = page JobCreatePage
        when:
        jobShowPage.go()
        jobShowPage.getJobActionDropdownButton().click()
        jobShowPage.getDuplicateJobToProjectButtonBy().click()
        then:
        jobShowPage.waitForModal(1)
        when:
        jobShowPage.selectProjectToDuplicateJob(duplicatedProjectName)
        jobShowPage.getDuplicateJobToProjectSubmitButton().click()
        jobCreatePage.saveJob()
        then:
        jobShowPage.currentUrl().contains(duplicatedProjectName)
        jobShowPage.currentUrl().contains("/job/show/")
        cleanup:
        deleteProject(projectName)
        deleteProject(duplicatedProjectName)
    }

}
