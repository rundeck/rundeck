package org.rundeck.tests.functional.selenium.scm

import org.rundeck.util.common.scm.ScmIntegration
import org.rundeck.util.gui.scm.ScmStatusBadge
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.scm.ConfigureScmPage
import org.rundeck.util.gui.pages.scm.PerformScmActionPage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.api.scm.GitScmApiClient
import org.rundeck.util.api.scm.gitea.GiteaApiRemoteRepo
import org.rundeck.util.api.storage.KeyStorageApiClient
import org.rundeck.util.container.SeleniumBase

@SeleniumCoreTest
class ScmStatusBadgeSpec extends SeleniumBase {

    private static String REPO_NAME = "statusBadgeTest"
    private static String PROJECT_LOCATION = "/projects-import/scm/PScmStatusBadgeTest.rdproject"
    private static String PROJECT_NAME = 'PScmStatusBadgeTest'
    private static GitScmApiClient scmApiClient

    @Override
    def setupSpec() {
        GiteaApiRemoteRepo repo = new GiteaApiRemoteRepo(REPO_NAME).setupRepo()
        repo.storeRepoPassInRundeck(new KeyStorageApiClient(clientProvider), "scm/scm.password")

        setupProjectArchiveDirectoryResource(PROJECT_NAME, PROJECT_LOCATION)
        scmApiClient = new GitScmApiClient(clientProvider).forProject(PROJECT_NAME)
        scmApiClient.forIntegration(ScmIntegration.EXPORT).callSetEnabledStatusForPlugin(false)
        scmApiClient.forIntegration(ScmIntegration.IMPORT).callSetEnabledStatusForPlugin(false)
    }

    def cleanup(){
        scmApiClient.forIntegration(ScmIntegration.EXPORT).callSetEnabledStatusForPlugin(false)
        scmApiClient.forIntegration(ScmIntegration.IMPORT).callSetEnabledStatusForPlugin(false)
    }

    def "job scm import status badge for a newly created job"(){
        setup:
        go(LoginPage).login(TEST_USER, TEST_PASS)
        ConfigureScmPage configureScmPage = go(ConfigureScmPage, PROJECT_NAME)
        configureScmPage.enableScmImport()

        JobShowPage jobShowPage = page(JobShowPage, PROJECT_NAME).forJob("740791d7-8734-4d8a-a77d-465aa2ccfe63")
        jobShowPage.go()

        when:
        ScmStatusBadge scmStatusBadge = jobShowPage.getScmStatusBadge()

        then:
        scmStatusBadge.iconClasses.containsAll(['glyphicon','glyphicon-question-sign'])
        scmStatusBadge.badgeText == 'Import Status: Not Tracked'
        scmStatusBadge.getTooltips() == 'Not Tracked for SCM Import'
    }

    def "job scm import status badge after import job changes"(){
        given:
        go(LoginPage).login(TEST_USER, TEST_PASS)
        final String jobUuid = "08879a4b-3d0d-427c-9b69-226296ce30af"
        ConfigureScmPage configureScmPage = go(ConfigureScmPage, PROJECT_NAME)
        configureScmPage.enableScmExport()

        page(PerformScmActionPage, PROJECT_NAME).commitJobChanges(jobUuid, "message example")
        configureScmPage.go()
        configureScmPage.disableScmExport()
        configureScmPage.enableScmImport()

        JobShowPage jobShowPage = page(JobShowPage, PROJECT_NAME).forJob(jobUuid)
        jobShowPage.go()

        when:
        ScmStatusBadge scmStatusBadge = jobShowPage.getScmStatusBadge()

        then:
        scmStatusBadge.iconClasses.containsAll(['glyphicon','glyphicon-exclamation-sign'])
        scmStatusBadge.badgeText == 'Import Needed'
        scmStatusBadge.getTooltips() == 'Import Status: Job changes need to be pulled'
    }


    def "job scm export status badge when job created and after commit"(){
        given:
        go(LoginPage).login(TEST_USER, TEST_PASS)
        final String jobUuid = "1ccd6bc5-63ae-4853-8f00-5fdaad031a24"
        ConfigureScmPage configureScmPage = go(ConfigureScmPage, PROJECT_NAME)
        configureScmPage.enableScmExport()

        JobShowPage jobShowPage = page(JobShowPage, PROJECT_NAME).forJob(jobUuid)
        jobShowPage.go()

        when:
        ScmStatusBadge scmStatusBadge = jobShowPage.getScmStatusBadge()

        then:
        scmStatusBadge.iconClasses.containsAll(['glyphicon','glyphicon-exclamation-sign'])
        scmStatusBadge.badgeText == 'Created'
        scmStatusBadge.getTooltips() == 'Export Status: New Job, Not yet added to SCM'

        when:
        page(PerformScmActionPage, PROJECT_NAME).commitJobChanges(jobUuid, "message example")
        jobShowPage.go()
        ScmStatusBadge scmStatusBadgeAfterCommit = jobShowPage.getScmStatusBadge()

        then:
        scmStatusBadgeAfterCommit.iconClasses.containsAll(['glyphicon','glyphicon-ok'])
        scmStatusBadgeAfterCommit.badgeText == 'No Change'
        scmStatusBadgeAfterCommit.getTooltips() == 'Export Status: Clean'
    }
}
