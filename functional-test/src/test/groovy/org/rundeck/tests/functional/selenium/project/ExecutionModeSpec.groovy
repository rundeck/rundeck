package org.rundeck.tests.functional.selenium.project

import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.common.navigation.NavLinkTypes
import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.jobs.JobListPage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.ProjectEditPage
import org.rundeck.util.gui.pages.project.SideBarPage

@SeleniumCoreTest
class ExecutionModeSpec extends SeleniumBase{

    /**
     * It disables executions via edit configuration file, then checks for the job to have the run button disabled
     */
    def "disable execution at project level"(){
        given:
            def projectName = "disabledExecutionsProject"
            setupProject(projectName)
            def projectEditPage = page ProjectEditPage
            def homePage = page HomePage
            def loginPage = page LoginPage
            def jobListPage = page JobListPage
            def sideBarPage = page SideBarPage
            def yamlJob = """
                        -
                          project: ${projectName}
                          loglevel: INFO
                          sequence:
                            keepgoing: false
                            strategy: node-first
                            commands:
                            - exec: echo hello there
                          description: ''
                          name: test-job
                        """
            def pathToJob = JobUtils.generateFileToImport(yamlJob, "yaml")
            def multipartBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("xmlBatch", new File(pathToJob).name, RequestBody.create(new File(pathToJob), MultipartBody.FORM))
                    .build()
            client.doPostWithMultipart("/project/${projectName}/jobs/import?format=yaml&dupeOption=skip", multipartBody)
        when:
            loginPage.go()
            loginPage.login(TEST_USER, TEST_PASS)
            homePage.validatePage()
            projectEditPage.go("/project/${projectName}/configure")
            projectEditPage.save()
            projectEditPage.go("/project/${projectName}/configure")
            projectEditPage.clickEditConfigurationFile()
            projectEditPage.replaceConfiguration("project.disable.executions=false", "project.disable.executions=true")
            projectEditPage.save()
            projectEditPage.validateConfigFileSave()
            sideBarPage.goTo NavLinkTypes.JOBS
        then:
            jobListPage.expectExecutionsDisabled()
        cleanup:
        deleteProject(projectName)
    }
}
