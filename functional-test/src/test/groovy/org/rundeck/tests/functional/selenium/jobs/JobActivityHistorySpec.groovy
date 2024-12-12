package org.rundeck.tests.functional.selenium.jobs
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.openqa.selenium.By
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.activity.ActivityPage
import org.rundeck.util.gui.pages.execution.ExecutionShowPage
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.login.LoginPage
@SeleniumCoreTest
class JobActivityHistorySpec extends SeleniumBase {
    def "Validate job activity history for a project"() {
        given:
        def projectName = "activityHistoryProject"
        def jobName = "test-activity-job"
        setupProject(projectName)
        // Prepare YAML job
        def yamlJob = """
                        -
                          project: ${projectName}
                          loglevel: INFO
                          sequence:
                            keepgoing: false
                            strategy: node-first
                            commands:
exec: echo "Activity History Test"
                          description: ''
                          name: ${jobName}
                        """
        def pathToJob = JobUtils.generateFileToImport(yamlJob, "yaml")
        def multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("xmlBatch", new File(pathToJob).name, RequestBody.create(new File(pathToJob), MultipartBody.FORM))
                .build()
        client.doPostWithMultipart("/project/${projectName}/jobs/import?format=yaml&dupeOption=skip", multipartBody)
        when:
        // Login and navigate to Job Show Page
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
        def jobShowPage = page(JobShowPage, projectName).forJob(jobName)
        jobShowPage.go()
        jobShowPage.runJobBtn.click()
        then:
        def executionShowPage = page ExecutionShowPage
        executionShowPage.validateStatus('SUCCEEDED')
        when:
        // Navigate to Activity Page
        def activityPage = go ActivityPage, projectName
        then:
        // Validate Activity History
        activityPage.validatePage()
        def activityRows = activityPage.getActivityRows()
        assert activityRows.size() > 0: "No activities found for the project"
        def firstRow = activityRows[0]
        assert firstRow.findElement(By.cssSelector(".exec-status.icon")).getAttribute("data-statusstring") == "succeeded"
        cleanup:
        deleteProject(projectName)
    }
}