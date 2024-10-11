package org.rundeck.tests.functional.selenium.project

import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.api.responses.execution.Execution
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.execution.ExecutionUtils
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.common.navigation.NavLinkTypes
import org.rundeck.util.gui.common.navigation.NavProjectSettings
import org.rundeck.util.gui.pages.activity.ActivityPage
import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.jobs.JobListPage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.ProjectEditPage
import org.rundeck.util.gui.pages.project.SideBarPage

@SeleniumCoreTest
class ExecutionModeSpec extends SeleniumBase{

    /**
     * It disables executions via edit configuration file, then checks for the job to have the run button disabled
     * then enables executions and checks for the run job button to be shown
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
            jobListPage.getCountJobList() == 0
        when:
            projectEditPage.go("/project/${projectName}/configure")
            projectEditPage.clickEditConfigurationFile()
            projectEditPage.replaceConfiguration("project.disable.executions=true", "project.disable.executions=false")
            projectEditPage.save()
            projectEditPage.validateConfigFileSave()
            sideBarPage.goTo NavLinkTypes.JOBS
        then:
            jobListPage.getCountJobList() == 1
        cleanup:
        deleteProject(projectName)
    }

    /**
     * It disables executions via UI, then checks for the job to have the run button disabled
     * then enables executions and checks for the run job button to be shown
     */
    def "disable execution at project level UI"(){
        given:
        def projectName = "disabledExecutionsProjectUI"
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
        projectEditPage.clickNavLink(NavProjectSettings.EXEC_MODE)
        projectEditPage.clickExecutionMode()
        projectEditPage.save()
        sideBarPage.goTo NavLinkTypes.JOBS
        then:
        jobListPage.expectExecutionsDisabled()
        jobListPage.getCountJobList() == 0
        when:
        projectEditPage.go("/project/${projectName}/configure")
        projectEditPage.clickNavLink(NavProjectSettings.EXEC_MODE)
        projectEditPage.clickExecutionMode()
        projectEditPage.save()
        sideBarPage.goTo NavLinkTypes.JOBS
        then:
        jobListPage.getCountJobList() == 1
        cleanup:
        deleteProject(projectName)
    }

    /**
     * It disables schedules via edit configuration file, then checks for the job to have the run button to be enabled
     * and pause icon to be shown (indicating that schedules are disabled)
     * then enables schedules and check the activity page
     */
    def "disable schedules at project level"(){
        given:
        def projectName = "disabledSchedulesProject"
        setupProject(projectName)
        def projectEditPage = page ProjectEditPage
        def homePage = page HomePage
        def loginPage = page LoginPage
        def jobListPage = page JobListPage
        def sideBarPage = page SideBarPage
        def activityPage = page ActivityPage
        def jobName = "test-job-schedules"
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
                          name: ${jobName}
                          schedule:
                            month: '*'
                            time:
                              hour: '*'
                              minute: '*'
                              seconds: '*/2'
                            weekday:
                              day: '*'
                            year: '*'
                          scheduleEnabled: true
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
        projectEditPage.replaceConfiguration("project.disable.schedule=false", "project.disable.schedule=true")
        projectEditPage.save()
        projectEditPage.validateConfigFileSave()
        sideBarPage.goTo NavLinkTypes.JOBS
        then:
        jobListPage.expectScheduleDisabled()
        when:
        // Waits for all executions to finish
        waitFor(ExecutionUtils.Retrievers.executionsForProject(client, projectName),
                verifyForAll(ExecutionUtils.Verifiers.executionFinished()))
        sideBarPage.goTo NavLinkTypes.ACTIVITY
        def executions = activityPage.getActivityRowsByJobName(jobName).size()
        projectEditPage.go("/project/${projectName}/configure")
        projectEditPage.clickEditConfigurationFile()
        projectEditPage.replaceConfiguration("project.disable.schedule=true", "project.disable.schedule=false")
        projectEditPage.save()
        projectEditPage.validateConfigFileSave()
        //This is to allow at least one execution to finish
        waitFor(ExecutionUtils.Retrievers.executionsForProject(client, projectName),
                { List<Execution> execs ->  execs.any(ExecutionUtils.Verifiers.executionRunning()) },
                WaitingTime.EXCESSIVE)
        // Waits for at least one execution to start running
        waitFor(ExecutionUtils.Retrievers.executionsForProject(client, projectName),
                verifyForAll(ExecutionUtils.Verifiers.executionFinished()),
                WaitingTime.EXCESSIVE)
        sideBarPage.goTo NavLinkTypes.ACTIVITY
        then:
        executions < activityPage.getActivityRowsByJobName(jobName).size()
        cleanup:
        updateConfigurationProject(projectName, [
                "project.disable.schedule": "true",
                "project.later.schedule.enable": "false",
                "project.disable.executions": "true"
        ])
        // Waits for all executions to finish
        waitFor(ExecutionUtils.Retrievers.executionsForProject(client, projectName),
                verifyForAll(ExecutionUtils.Verifiers.executionFinished()))
        deleteProject(projectName)
    }

    /**
     * It disables schedules via UI, then checks for the job to have the run button to be enabled
     * and pause icon to be shown (indicating that schedules are disabled)
     * then enables schedules and check the activity page
     */
    def "disable schedules at project level UI"(){
        given:
        def projectName = "disabledSchedulesProjectUI"
        setupProject(projectName)
        def projectEditPage = page ProjectEditPage
        def homePage = page HomePage
        def loginPage = page LoginPage
        def jobListPage = page JobListPage
        def sideBarPage = page SideBarPage
        def activityPage = page ActivityPage
        def jobName = "test-job-schedules"
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
                          name: ${jobName}
                          schedule:
                            month: '*'
                            time:
                              hour: '*'
                              minute: '*'
                              seconds: '*/2'
                            weekday:
                              day: '*'
                            year: '*'
                          scheduleEnabled: true
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
        projectEditPage.clickNavLink(NavProjectSettings.EXEC_MODE)
        projectEditPage.clickScheduleMode()
        projectEditPage.save()
        sideBarPage.goTo NavLinkTypes.JOBS
        then:
        jobListPage.expectScheduleDisabled()
        when:
        // Waits for all executions to finish
        waitFor(ExecutionUtils.Retrievers.executionsForProject(client, projectName),
                verifyForAll(ExecutionUtils.Verifiers.executionFinished()))
        sideBarPage.goTo NavLinkTypes.ACTIVITY
        def executions = activityPage.getActivityRowsByJobName(jobName).size()
        projectEditPage.go("/project/${projectName}/configure")
        projectEditPage.clickNavLink(NavProjectSettings.EXEC_MODE)
        projectEditPage.clickScheduleMode()
        projectEditPage.save()
        // Waits for at least one execution to start running
        waitFor(ExecutionUtils.Retrievers.executionsForProject(client, projectName),
                { List<Execution> execs ->  execs.any(ExecutionUtils.Verifiers.executionRunning()) },
                WaitingTime.EXCESSIVE)
        // Waits for all executions to finish
        waitFor(ExecutionUtils.Retrievers.executionsForProject(client, projectName),
                verifyForAll(ExecutionUtils.Verifiers.executionFinished()))
        sideBarPage.goTo NavLinkTypes.ACTIVITY
        then:
        executions < activityPage.getActivityRowsByJobName(jobName).size()
        cleanup:
        updateConfigurationProject(projectName, [
                "project.disable.schedule": "true",
                "project.later.schedule.enable": "false",
                "project.disable.executions": "true"
        ])
        // Waits for all executions to finish
        waitFor(ExecutionUtils.Retrievers.executionsForProject(client, projectName),
                verifyForAll(ExecutionUtils.Verifiers.executionFinished()))
        deleteProject(projectName)
    }
}
