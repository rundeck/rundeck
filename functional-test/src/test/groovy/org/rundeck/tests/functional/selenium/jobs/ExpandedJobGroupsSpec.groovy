package org.rundeck.tests.functional.selenium.jobs


import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.jobs.*
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.ProjectCreatePage
import org.rundeck.util.gui.pages.project.ProjectEditPage
import spock.lang.Stepwise

@SeleniumCoreTest
@Stepwise
class ExpandedJobGroupsSpec extends SeleniumBase {

    def setup() {
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
    }

    /**
     * It logs in as admin, creates a new project with the class name, empty description and empty label
     * Then creates 4 jobs and groups them into different level layers
     * Then goes to project settings, sets Job Group Expanded Levels to -1, 0, 1, 2 and validates Job Groups are expanded/hidden accordingly
     * Then deletes the project and logs out
     */

    def "Job Groups are expanded or hidden accordingly"(){
        when:
        def expandedGroupsProp = "project.jobs.gui.groupExpandLevel="

        String projectName = "ExpandedJobGroupsSpec"
        def homePage = go HomePage
        homePage.validatePage()

        // Project create
        def projectCreatePage = go ProjectCreatePage
        projectCreatePage.validatePage()
        projectCreatePage.createProject(projectName)
        projectCreatePage.waitForUrlToContain("project/${projectName}/nodes/sources")

        // Creates job 1
        JobCreatePage jobCreatePage = page JobCreatePage
        jobCreatePage.loadCreatePath(projectName)
        jobCreatePage.go()

        jobCreatePage.jobNameInput.sendKeys("Level0")
        jobCreatePage.tab JobTab.WORKFLOW click()
        jobCreatePage.executeScript "window.location.hash = '#addnodestep'"
        jobCreatePage.stepLink 'exec-command', StepType.NODE click()
        jobCreatePage.waitForElementVisible jobCreatePage.adhocRemoteStringField
        jobCreatePage.adhocRemoteStringField.click()
        jobCreatePage.waitForNumberOfElementsToBeOne jobCreatePage.floatBy
        jobCreatePage.adhocRemoteStringField.sendKeys "echo 'this is a parent job'"
        jobCreatePage.saveStep 0
        jobCreatePage.createJobButton.click()

        // Extract Job 1 uuid for duplication later
        JobShowPage jobShowPage = page JobShowPage
        def firstJobUuid = jobShowPage.getJobUuid().text

        // Creates job 2 (Duplicates job 1)
        CreateDuplicatedJobPage createDuplicatedJobPage = page CreateDuplicatedJobPage
        createDuplicatedJobPage.loadDuplicatedJobPath(projectName, firstJobUuid)
        createDuplicatedJobPage.go()
        createDuplicatedJobPage.jobNameInput.clear()
        createDuplicatedJobPage.jobNameInput.sendKeys("Level1")
        createDuplicatedJobPage.jobGroupField.sendKeys("Parent")
        createDuplicatedJobPage.createJobButton.click()

        // Extract Job 2 uuid for duplication later
        def secondJobUuid = jobShowPage.getJobUuid().text

        // Creates job 3 (Duplicates job 2)
        createDuplicatedJobPage.loadDuplicatedJobPath(projectName, secondJobUuid)
        createDuplicatedJobPage.go()
        createDuplicatedJobPage.jobNameInput.clear()
        createDuplicatedJobPage.jobNameInput.sendKeys("Level2")
        createDuplicatedJobPage.jobGroupField.clear()
        createDuplicatedJobPage.jobGroupField.sendKeys("Parent/Child")
        createDuplicatedJobPage.createJobButton.click()

        // Extract Job 3 uuid for duplication later
        def thirdJobUuid = jobShowPage.getJobUuid().text

        // Creates job 4 (Duplicates job 3)
        createDuplicatedJobPage.loadDuplicatedJobPath(projectName, thirdJobUuid)
        createDuplicatedJobPage.go()
        createDuplicatedJobPage.jobNameInput.clear()
        createDuplicatedJobPage.jobNameInput.sendKeys("Level3")
        createDuplicatedJobPage.jobGroupField.clear()
        createDuplicatedJobPage.jobGroupField.sendKeys("Parent/Child/SubChild")
        createDuplicatedJobPage.createJobButton.click()

        // Edit Project Configuration to expand the job groups by default
        ProjectEditPage projectEditPage = page ProjectEditPage
        projectEditPage.loadProjectEditForProject(projectName)
        projectEditPage.go()
        projectEditPage.clickEditConfigurationFile()
        projectEditPage.replaceConfiguration("${expandedGroupsProp}1","${expandedGroupsProp}-1")
        projectEditPage.save()

        // Go to jobs list and check if all the grouped jobs are expanded
        JobListPage jobListPage = page JobListPage
        jobListPage.loadJobListForProject(projectName)
        jobListPage.go()
        def jobGroupsExpanded = jobListPage.getExpandedJobGroupsContainer().text

        then: "We check if the job groups are expanded"
        jobGroupsExpanded == "Parent\nLevel1\nChild\nLevel2\nSubChild\nLevel3"

        when: "We change the expand level to 0"
        projectEditPage.go()
        projectEditPage.clickEditConfigurationFile()
        projectEditPage.replaceConfiguration("${expandedGroupsProp}-1","${expandedGroupsProp}0")
        projectEditPage.save()

        jobListPage.go()

        then: "Job groups aren't expanded"
        jobListPage.getExpandedJobGroupsContainerChildren().size() < 1

        when: "We change teh expand level to 1"
        projectEditPage.go()
        projectEditPage.clickEditConfigurationFile()
        projectEditPage.replaceConfiguration("${expandedGroupsProp}0","${expandedGroupsProp}1")
        projectEditPage.save()

        jobListPage.go()

        then: "Job groups expanded"
        jobListPage.getExpandedJobGroupsContainerChildren().size() < 2

        when: "We change teh expand level to 2"
        projectEditPage.go()
        projectEditPage.clickEditConfigurationFile()
        projectEditPage.replaceConfiguration("${expandedGroupsProp}1","${expandedGroupsProp}2")
        projectEditPage.save()

        jobListPage.go()

        then: "Job groups expanded"
        jobListPage.getExpandedJobGroupsContainerChildren().size() < 3

        cleanup:
        deleteProject(projectName)

    }
}
