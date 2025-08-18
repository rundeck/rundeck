package org.rundeck.tests.functional.selenium.scm

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.api.scm.GitScmApiClient
import org.rundeck.util.api.scm.gitea.GiteaApiRemoteRepo
import org.rundeck.util.api.storage.KeyStorageApiClient
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.scm.ScmIntegration
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.scm.ConfigureScmPage
import org.rundeck.util.gui.pages.scm.PerformScmActionPage

@SeleniumCoreTest
class ScmImportSpec extends SeleniumBase  {
    private static String REPO_NAME = "importScmTest"
    private static String PROJECT_LOCATION = "/projects-import/scm/PScmImportTest.rdproject"
    private static String PROJECT_NAME = 'PScmImportTest'
    private static GitScmApiClient scmApiClient
    private static GiteaApiRemoteRepo repo;

    @Override
    def setupSpec() {
        repo = new GiteaApiRemoteRepo(REPO_NAME).setupRepo()
        repo.storeRepoPassInRundeck(new KeyStorageApiClient(clientProvider), "scm/scm.password")

        setupProjectArchiveDirectoryResource(PROJECT_NAME, PROJECT_LOCATION)
        scmApiClient = new GitScmApiClient(clientProvider).forProject(PROJECT_NAME)
        scmApiClient.forIntegration(ScmIntegration.IMPORT).callSetEnabledStatusForPlugin(false)
    }

    def cleanup(){
        scmApiClient.forIntegration(ScmIntegration.IMPORT).callSetEnabledStatusForPlugin(false)
    }

    def "job scm import with quotes on file name"(){
        setup:
        go(LoginPage).login(TEST_USER, TEST_PASS)
        // Generate an XML definition for a scheduled job
        def pathXmlFile = getClass().getResource("/test-files/aaasT1.xml").getPath()
        def xmlContent = new File(pathXmlFile).text

        // Create the job file in the git repo
        repo.createFile("\"aaa s\" T1.xml", xmlContent)

        ConfigureScmPage configureScmPage = go(ConfigureScmPage, PROJECT_NAME)
        configureScmPage.enableScmImport()

        Thread.sleep(WaitingTime.EXCESSIVE.toMillis())
        when:
        List<String> chosenTrackedItems = page(PerformScmActionPage, PROJECT_NAME).getChosenTrackedItems()

        then:
        chosenTrackedItems.contains("\"aaa s\" T1.xml")
    }
}
