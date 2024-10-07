package org.rundeck.tests.functional.selenium.jobs

import org.rundeck.util.annotations.ExcludePro
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.api.responses.execution.Execution
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.login.LoginPage

@SeleniumCoreTest
class JobSummarySpec extends SeleniumBase{

    def setup() {
        go(LoginPage).login(TEST_USER, TEST_PASS)
    }

    @ExcludePro
    /**
     * Checks for job stats
     * It's excluded from pro since the locators are different
     *
     */
    def "job show summary stats"(){
        given:
        String projectName = "summaryProject"
        setupProject(projectName)
        String jobUuid = JobUtils.jobImportFile(projectName, '/test-files/test.xml', client).succeeded.first().id
        def result = JobUtils.executeJobWithOptions(jobUuid, client, null)
        assert result.successful
        Execution execution = MAPPER.readValue(result.body().string(), Execution.class)
        waitForExecutionFinish(execution.id)
        JobShowPage jobShowPage = page(JobShowPage, projectName).forJob(jobUuid)
        when:
        jobShowPage.go()
        def statsElements = jobShowPage.getJobStatsElements()
        then:
        statsElements.size() == 3
        statsElements.get(0).getText().contains("EXECUTIONS")
        statsElements.get(1).getText().contains("SUCCESS RATE")
        statsElements.get(2).getText().contains("AVG DURATION")
        cleanup:
        deleteProject(projectName)
    }

}
