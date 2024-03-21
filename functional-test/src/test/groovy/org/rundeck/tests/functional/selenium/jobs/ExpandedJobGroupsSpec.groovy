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
        given:
        String projectName = "ExpandedJobGroupsSpec"
        setupProjectArchiveDirectoryResource(
                projectName,
                '/projects-import/expanded-job-groups'
        )
        def expandedGroupsProp = "project.jobs.gui.groupExpandLevel="

        when:
        // Edit Project Configuration to expand the job groups by default
        ProjectEditPage projectEditPage = page ProjectEditPage
        projectEditPage.loadProjectEditForProject(projectName)
        projectEditPage.go()
        projectEditPage.clickEditConfigurationFile()
        projectEditPage.replaceConfiguration("${expandedGroupsProp}1","${expandedGroupsProp}-1")
        projectEditPage.save()

        JobListPage jobListPage = page JobListPage
        jobListPage.loadJobListForProject(projectName)
        jobListPage.go()
        jobListPage.waitForElementToBeClickable(jobListPage.getExpandedJobGroupsContainer())
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
