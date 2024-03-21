package org.rundeck.tests.functional.selenium.jobs.steps

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.execution.ExecutionShowPage
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.login.LoginPage

@SeleniumCoreTest
class LogDataStepSpec extends SeleniumBase{
    static final By TABLE_SELECTOR = By.cssSelector(".table.table-condensed.table-bordered.table-embed.table-data-embed")

    def setup() {
        go(LoginPage).login(TEST_USER, TEST_PASS)
    }

    def "log data workflow step runs successfully and shows context values"(){
        setup:
        String projectName = 'LogDataStepTest'
        setupProjectArchiveDirectoryResource(projectName, "/projects-import/${projectName}.rdproject")
        JobShowPage jobPage = page(JobShowPage, projectName).forJob('1b560606-909d-43c0-aaa6-77894f2fc952')
        jobPage.go()

        when:
        ExecutionShowPage executionPage = jobPage.runJob(true)

        then:
        List<WebElement> logOutput = executionPage.getLogOutput()
        verifyAll {
            executionPage.getExecutionStatus() == 'SUCCEEDED'
            logOutput[0].getText() == 'This is a Sample Job'

            logOutput[1].getText().contains('wasRetry')
            logOutput[1].getText().contains('executionType')
            logOutput[1].getText().contains('successOnEmptyNodeFilter')
            logOutput[1].getText().contains('retryInitialExecId')

            logOutput[1].findElements(TABLE_SELECTOR).size() == 2
            logOutput[2].findElements(TABLE_SELECTOR).size() == 2
            logOutput[3].findElements(TABLE_SELECTOR).size() == 2
        }
    }
}
