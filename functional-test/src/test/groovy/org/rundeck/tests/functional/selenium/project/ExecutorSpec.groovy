package org.rundeck.tests.functional.selenium.project

import org.rundeck.util.annotations.ExcludePro
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.DashboardPage
import org.rundeck.util.gui.pages.project.ProjectEditPage

import java.util.stream.Collectors

@SeleniumCoreTest
class ExecutorSpec extends SeleniumBase {

    private static final List<String> availableExecutors = Arrays.asList(new String[] { "SSH", "Local", "Local (New)", "SSHJ-SSH", "Script Execution", "Stub", "Ansible Ad-Hoc Node Executor", "WinRM Node Executor Python", "openssh / executor" });
    private static final int EXPECTED_EXECUTORS_SIZE = availableExecutors.size()
    /**
     * Checks if the node executor list renders.
     *
     */
    @ExcludePro
    def "Check node executor list"(){
        given:
        def projectName = "filter-nodes-job-edit-test"
        setupProject(projectName)
        LoginPage loginPage = page LoginPage
        HomePage homePage = page HomePage
        ProjectEditPage projectEditPage = page ProjectEditPage

        when: "We check the node executors length"
        loginPage.go()
        loginPage.login(TEST_USER, TEST_PASS)
        homePage.validatePage()
        projectEditPage.loadProjectEditForProject(projectName)
        projectEditPage.go()
        projectEditPage.defaultNodeExecutorLink.click()
        projectEditPage.nodeExecutorDropdown.click()
        projectEditPage.waitForElementVisible(projectEditPage.openedNodeExecutorDropdown)
        def executorsInProject = projectEditPage.getExecutors()
        def executorsToList = executorsInProject.stream().map {
            it.getText()
        }.collect(Collectors.toList())

        then: "We check if the size isn't zero and if the list contains all available node executors"
        executorsInProject.size() == EXPECTED_EXECUTORS_SIZE
        availableExecutors.containsAll(executorsToList)

        cleanup:
        deleteProject(projectName)

    }

    /**
     * Checks if the SSH node executor configuration persists.
     *
     */
    def "Save project SSH configuration"(){
        given:
        def projectName = "ssh-executor-saved"
        setupProject(projectName)
        LoginPage loginPage = page LoginPage
        HomePage homePage = page HomePage
        ProjectEditPage projectEditPage = page ProjectEditPage
        DashboardPage dashboardPage = page DashboardPage
        def sampleKeyStoragePath = "keys/example/exampleKey.key"

        when: "We check if the configuration of the SSH node exec is persisted"
        loginPage.go()
        loginPage.login(TEST_USER, TEST_PASS)
        homePage.validatePage()
        projectEditPage.loadProjectEditForProject(projectName)
        projectEditPage.go()
        projectEditPage.defaultNodeExecutorLink.click()
        projectEditPage.nodeExecutorDropdown.click()
        projectEditPage.waitForElementVisible(projectEditPage.openedNodeExecutorDropdown)
        projectEditPage.sshListItem.click()
        projectEditPage.defaultKeyFilepath.sendKeys(sampleKeyStoragePath)
        projectEditPage.defaultConfigKeyStoragePathInput.sendKeys(sampleKeyStoragePath)
        projectEditPage.defaultPasswordStoragePath.sendKeys(sampleKeyStoragePath)
        projectEditPage.defaultPassphraseStoragePath.sendKeys(sampleKeyStoragePath)
        projectEditPage.alwaysSetPty.click()
        projectEditPage.connectionTimeout.clear()
        projectEditPage.connectionTimeout.sendKeys("3000")
        projectEditPage.sshCommandTimeout.clear()
        projectEditPage.sshCommandTimeout.sendKeys("2000")
        projectEditPage.sshBindAddress.sendKeys("example/bind/address")
        projectEditPage.passRdEnv.click()
        projectEditPage.save()
        dashboardPage.validatePage()
        projectEditPage.go()
        projectEditPage.defaultNodeExecutorLink.click()
        projectEditPage.nodeExecutorDropdown.click()
        projectEditPage.waitForElementVisible(projectEditPage.openedNodeExecutorDropdown)
        projectEditPage.sshListItem.click()

        then: "check each input"
        projectEditPage.defaultKeyFilepath.getAttribute("value") == sampleKeyStoragePath
        projectEditPage.defaultConfigKeyStoragePathInput.getAttribute("value") == sampleKeyStoragePath
        projectEditPage.defaultPasswordStoragePath.getAttribute("value") == sampleKeyStoragePath
        projectEditPage.defaultPassphraseStoragePath.getAttribute("value") == sampleKeyStoragePath
        projectEditPage.alwaysSetPty.isSelected()
        projectEditPage.connectionTimeout.getAttribute("value") == "3000"
        projectEditPage.sshCommandTimeout.getAttribute("value") == "2000"
        projectEditPage.sshBindAddress.getAttribute("value") == "example/bind/address"
        projectEditPage.passRdEnv.isSelected()

        cleanup:
        deleteProject(projectName)

    }

    /**
     * Checks if the Local executor configuration persists.
     *
     */
    def "Save local executor"(){
        given:
        def projectName = "local-executor-saved"
        setupProject(projectName)
        LoginPage loginPage = page LoginPage
        HomePage homePage = page HomePage
        ProjectEditPage projectEditPage = page ProjectEditPage
        DashboardPage dashboardPage = page DashboardPage

        when: "We check if the configuration of the SSH node exec is persisted"
        loginPage.go()
        loginPage.login(TEST_USER, TEST_PASS)
        homePage.validatePage()
        projectEditPage.loadProjectEditForProject(projectName)
        projectEditPage.go()
        projectEditPage.defaultNodeExecutorLink.click()
        projectEditPage.nodeExecutorDropdown.click()
        projectEditPage.localExecutorListItem.click()
        projectEditPage.save()
        dashboardPage.validatePage()
        projectEditPage.go()
        projectEditPage.defaultNodeExecutorLink.click()

        then: "No errors shown and the local exec is selected by default"
        projectEditPage.dangerMessages.size() == 0
        projectEditPage.nodeExecutorDropdown.text.startsWith("Local")

        cleanup:
        deleteProject(projectName)

    }

    /**
     * Checks if the script executor configuration persists.
     *
     */
    def "Save script executor"(){
        given:
        def projectName = "script-executor-saved"
        setupProject(projectName)
        LoginPage loginPage = page LoginPage
        HomePage homePage = page HomePage
        ProjectEditPage projectEditPage = page ProjectEditPage
        DashboardPage dashboardPage = page DashboardPage

        when: "We check if the configuration of the script node exec is persisted"
        loginPage.go()
        loginPage.login(TEST_USER, TEST_PASS)
        homePage.validatePage()
        projectEditPage.loadProjectEditForProject(projectName)
        projectEditPage.go()
        projectEditPage.defaultNodeExecutorLink.click()
        projectEditPage.nodeExecutorDropdown.click()
        projectEditPage.scriptExecutorListItem.click()
        projectEditPage.scriptExecutorConfigCommandInput.sendKeys("exampleCommand")
        projectEditPage.scriptExecutorConfigInterpreter.sendKeys("exampleInterpreter")
        projectEditPage.scriptExecutorConfigDirectory.sendKeys("/example/dir")
        projectEditPage.save()
        dashboardPage.validatePage()
        projectEditPage.go()
        projectEditPage.defaultNodeExecutorLink.click()
        projectEditPage.nodeExecutorDropdown.click()

        then: "Dropdown shows the right config and all previous inputs have values"
        projectEditPage.nodeExecutorDropdown.text.startsWith("Script")
        projectEditPage.scriptExecutorConfigCommandInput.getAttribute("value") == "exampleCommand"
        projectEditPage.scriptExecutorConfigInterpreter.getAttribute("value") == "exampleInterpreter"
        projectEditPage.scriptExecutorConfigDirectory.getAttribute("value") == "/example/dir"

        cleanup:
        deleteProject(projectName)

    }

    /**
     * Checks if the SSHJ node executor configuration persists.
     *
     */
    def "Save SSHJ config"(){
        given:
        def projectName = "sshj-executor-saved"
        setupProject(projectName)
        LoginPage loginPage = page LoginPage
        HomePage homePage = page HomePage
        ProjectEditPage projectEditPage = page ProjectEditPage
        DashboardPage dashboardPage = page DashboardPage
        def samplePath = "keys/example/exampleKey.key"

        when: "We check if the configuration of the SSHJ node exec is persisted"
        loginPage.go()
        loginPage.login(TEST_USER, TEST_PASS)
        homePage.validatePage()
        projectEditPage.loadProjectEditForProject(projectName)
        projectEditPage.go()
        projectEditPage.defaultNodeExecutorLink.click()
        projectEditPage.nodeExecutorDropdown.click()
        projectEditPage.defaultConfigKeyStoragePathInput.sendKeys(samplePath)
        projectEditPage.defaultPasswordStoragePath.sendKeys(samplePath)
        projectEditPage.defaultPassphraseStoragePath.sendKeys(samplePath)
        projectEditPage.defaultKeyFilepath.sendKeys(samplePath)
        projectEditPage.retryEnableInput.click()
        projectEditPage.retryCounterInput.clear()
        projectEditPage.retryCounterInput.sendKeys("3000")
        projectEditPage.save()
        dashboardPage.validatePage()
        projectEditPage.go()
        projectEditPage.defaultNodeExecutorLink.click()

        then: "All config persisted"
        projectEditPage.nodeExecutorDropdown.text.startsWith("SSHJ-SSH")
        projectEditPage.defaultConfigKeyStoragePathInput.getAttribute("value") == samplePath
        projectEditPage.defaultPasswordStoragePath.getAttribute("value") == samplePath
        projectEditPage.defaultPassphraseStoragePath.getAttribute("value") == samplePath
        projectEditPage.defaultKeyFilepath.getAttribute("value") == samplePath
        projectEditPage.retryCounterInput.getAttribute("value") == "3000"
        projectEditPage.retryEnableInput.isSelected()

        cleanup:
        deleteProject(projectName)

    }

}
