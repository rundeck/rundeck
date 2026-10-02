package org.rundeck.tests.functional.selenium.jobs

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.TopMenuPage
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.jobs.JobTab
import org.rundeck.util.gui.pages.login.LoginPage

/**
 * A Quartz trigger runs a scheduled job as the user saved on the job, without
 * re-checking that this user is still allowed to run it. A job can therefore be
 * scheduled to execute under an identity that has lost -- or never had -- run access.
 *
 * These tests cover the non-blocking warning that surfaces that state on the job page.
 * The permission combination is produced the only way it can be: a user who may author
 * jobs but not run them creates one through the UI, so the saved owner is that user.
 *
 * ACLs: {@code ScheduledJobExecutionAclWarningSpec.aclpolicy} grants AuthTest1 project
 * read, job create/read/update/delete and node read -- deliberately no run.
 *
 * The job show page is not gated on a UI mode, so these tests pin the default UI.
 */
@SeleniumCoreTest
class ScheduledJobExecutionAclWarningSpec extends SeleniumBase {

    static final String PROJECT_NAME = 'ScheduledJobExecutionAclWarningSpec'
    public static final String ACLPOLICY_FILE = PROJECT_NAME + ".aclpolicy"
    static final String RESTRICTED_USER = 'AuthTest1'
    static final String USER_PASSWORD = 'password'

    def setupSpec() {
        setupProject(PROJECT_NAME)
        importSystemAcls("/${ACLPOLICY_FILE}", ACLPOLICY_FILE)
    }

    def cleanupSpec() {
        deleteProject(PROJECT_NAME)
        deleteSystemAcl(ACLPOLICY_FILE)
    }

    def "scheduled job saved by a user without run access warns on the job page"() {
        given: "a user who may author jobs but not run them schedules one"
            def login = page LoginPage
            login.go()
            login.login(RESTRICTED_USER, USER_PASSWORD)
            waitForPageLoadComplete()

            def jobCreatePage = go JobCreatePage, PROJECT_NAME
            jobCreatePage.fillBasicJob 'job scheduled by a user who cannot run it'
            jobCreatePage.tab JobTab.SCHEDULE click()
            jobCreatePage.scheduleRunYesField.click()
            if (!jobCreatePage.scheduleEveryDayCheckboxField.isSelected()) {
                jobCreatePage.scheduleEveryDayCheckboxField.click()
            }
            jobCreatePage.createJobButton.click()
            jobCreatePage.waitForUrlToContain('/job/show')

        and: "the uuid is read off the job page the save lands on"
            def ownerView = page JobShowPage
            ownerView.waitForElementVisible(ownerView.jobUuid)
            String jobUuid = ownerView.jobUuid.text

        expect: "the owner is told, and is named as the identity the schedule will use"
            ownerView.executionAclWarning.text.contains(RESTRICTED_USER)

        and: "but is offered no remediation link, having no access to edit project ACLs"
            ownerView.els(ownerView.executionAclFixLinkBy).size() == 0

        when: "an admin, who can edit project ACLs, opens the same job"
            def ownerTopMenu = page TopMenuPage
            ownerTopMenu.logOut()
            waitForPageLoadComplete()
            def adminLogin = page LoginPage
            adminLogin.go()
            adminLogin.login(TEST_USER, TEST_PASS)
            waitForPageLoadComplete()
            def adminView = page(JobShowPage, PROJECT_NAME).forJob(jobUuid)
            adminView.go()

        then: "the job name carries the badge and the banner names the saved owner"
            adminView.executionAclWarningBadge.isDisplayed()
            adminView.executionAclWarning.text.contains(RESTRICTED_USER)

        and: "the remediation link is offered this time"
            adminView.executionAclFixLink.isDisplayed()

        when: "the remediation link is followed"
            adminView.executionAclFixLink.click()

        then: "it opens the project ACL editor, pre-filled with the granting policy"
            adminView.waitForUrlToContain('createProjectAclFile')

        cleanup:
            def topMenuPage = page TopMenuPage
            topMenuPage.logOut()
            waitForPageLoadComplete()
    }

    def "scheduled job saved by a user who can run it shows no warning"() {
        given: "admin, who may run jobs, owns a scheduled job"
            def login = page LoginPage
            login.go()
            login.login(TEST_USER, TEST_PASS)
            waitForPageLoadComplete()

            // a quiet schedule: this job exists to be looked at, not to fire during the run
            def jobXml = JobUtils.generateScheduledJobsXml(
                'job scheduled by a user who can run it',
                "<time hour='3' seconds='0' minute='0' />"
            )
            def jobUuid = JobUtils.createJob(PROJECT_NAME, jobXml, client).succeeded.first().id

        when: "the job page is opened"
            def jobShowPage = page(JobShowPage, PROJECT_NAME).forJob(jobUuid)
            jobShowPage.go()
            // absence is only meaningful once the page has actually rendered
            jobShowPage.waitForElementVisible(jobShowPage.jobUuid)

        then: "neither the banner nor the header badge is rendered"
            jobShowPage.els(jobShowPage.executionAclWarningBy).size() == 0
            jobShowPage.els(jobShowPage.executionAclWarningBadgeBy).size() == 0

        cleanup:
            def topMenuPage = page TopMenuPage
            topMenuPage.logOut()
            waitForPageLoadComplete()
    }
}
