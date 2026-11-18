package org.rundeck.tests.functional.selenium.jobs

import org.rundeck.util.annotations.ExcludePro
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.annotations.UiMechanism
import org.rundeck.util.annotations.UiModeFlag
import org.rundeck.util.annotations.UiModeStatus
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.UiModes
import org.rundeck.util.gui.pages.TopMenuPage
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.jobs.JobListPage
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.ProjectAclCreatePage

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
    static final UI_MODES = UiModes.nextUiAndDefault()

    def setupSpec() {
        setupProject(PROJECT_NAME)
        importSystemAcls("/${ACLPOLICY_FILE}", ACLPOLICY_FILE)
    }

    def cleanupSpec() {
        deleteProject(PROJECT_NAME)
        deleteSystemAcl(ACLPOLICY_FILE)
    }

    /**
     * Excluded from Pro: it builds the job through the workflow editor, which Pro replaces
     * with the early-access editor. Pro runs its own copy, {@code ScheduledJobExecutionAclWarningProSpec}.
     */
    @ExcludePro
    @UiModeFlag(
        featureName = "menu-create-project-acl-file",
        status = UiModeStatus.NEXT_UI,
        mechanism = UiMechanism.COOKIE,
        jiraTicket = "RUN-0000",
        description = "ACL remediation opens both project ACL UIs selected by the nextUi cookie."
    )
    def "scheduled job saved by a user without run access warns on the job page"() {
        given: "a user who may author jobs but not run them schedules one"
            // per-iteration name: each iteration must be identifiable on its own, and cleanup
            // removes it so the next iteration's job list holds only its own job
            String jobName = "job scheduled by a user who cannot run it ${nextUi ? 'next' : 'legacy'}"
            def login = page LoginPage
            login.go()
            login.login(RESTRICTED_USER, USER_PASSWORD)
            waitForPageLoadComplete()

            def jobCreatePage = go JobCreatePage, PROJECT_NAME
            jobCreatePage.fillBasicJob jobName
            jobCreatePage.enableDailySchedule()
            jobCreatePage.clickCreateJob()
            jobCreatePage.waitForUrlToContain('/job/show')

        and: "the uuid is read off the job page the save lands on"
            def ownerView = page JobShowPage
            ownerView.waitForElementVisible(ownerView.jobUuid)
            String jobUuid = ownerView.jobUuid.text

        expect: "the owner is told, and is named as the identity the schedule will use"
            ownerView.executionAclWarning.text.contains(RESTRICTED_USER)

        and: "but is offered no remediation link, having no access to edit project ACLs"
            !ownerView.hasExecutionAclFixLink()

        when: "an admin, who can edit project ACLs, opens the project's job list"
            def ownerTopMenu = page TopMenuPage
            ownerTopMenu.logOut()
            waitForPageLoadComplete()
            def adminLogin = page LoginPage
            adminLogin.go()
            adminLogin.login(TEST_USER, TEST_PASS)
            waitForPageLoadComplete()
            def jobListPage = page JobListPage
            jobListPage.loadJobListForProject(PROJECT_NAME)
            jobListPage.go()

        then: "the job is marked in the list too, not only on its own page"
            jobListPage.executionAclWarningBadges.size() == 1

        when: "the admin opens the job itself"
            def adminView = page(JobShowPage, PROJECT_NAME).forJob(jobUuid)
            adminView.go()

        then: "the job name carries the badge and the banner names the saved owner"
            adminView.executionAclWarningBadge.isDisplayed()
            adminView.executionAclWarning.text.contains(RESTRICTED_USER)

        and: "the remediation link is offered this time"
            adminView.executionAclFixLink.isDisplayed()

        when: "the remediation link is followed"
            adminView.setNextUiCookie(nextUi)
            adminView.openExecutionAclFixLink()

        then: "it opens the project ACL editor, pre-filled with the granting policy"
            def aclCreatePage = page(ProjectAclCreatePage, PROJECT_NAME, [nextUi: nextUi])
            aclCreatePage.validatePage()
            aclCreatePage.editorText.contains("Allow ${RESTRICTED_USER} to run job")
            aclCreatePage.editorText.contains('allow:')
            aclCreatePage.editorText.contains('- run')
            aclCreatePage.editorText.contains("urn: user:${RESTRICTED_USER}")

        cleanup:
            // the warning-badge count above sees every job in the project, so this iteration's
            // job must not survive into the next one; API-only, so it runs before any UI step can throw
            deleteProjectJobs()
            def topMenuPage = page TopMenuPage
            // the cookie selected this iteration's ACL editor; don't let it outlive the iteration
            topMenuPage.deleteNextUiCookie()
            topMenuPage.logOut()
            waitForPageLoadComplete()

        where:
            [nextUi] << UI_MODES
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
            !jobShowPage.hasExecutionAclWarning()
            !jobShowPage.hasExecutionAclWarningBadge()

        cleanup:
            def topMenuPage = page TopMenuPage
            topMenuPage.logOut()
            waitForPageLoadComplete()
    }

    /**
     * Deletes every job in the spec's project through the API. Listing the project rather than
     * tracking a uuid also removes a job whose iteration failed before its uuid was read.
     */
    private void deleteProjectJobs() {
        def jobs = get("/project/${PROJECT_NAME}/jobs", List)
        jobs.each { job ->
            def response = doDelete("/job/${(job as Map).id}")
            assert response.successful : "could not delete job ${(job as Map).id}: ${response.code()}"
        }
    }
}
