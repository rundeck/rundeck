package org.rundeck.tests.functional.selenium.execution

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.execution.CommandPage
import org.rundeck.util.gui.pages.execution.ExecutionShowPage
import org.rundeck.util.gui.pages.login.LoginPage
import spock.lang.Unroll

@SeleniumCoreTest
class CommandOnMultipleNodesSpec extends SeleniumBase{

    public static final String TEST_PROJECT = "core-jsch-executor-test"
    public static final String TEST_SSH_ARCHIVE_DIR = "/projects-import/core-jsch-executor-test"
    public static final String NODE_KEY_PASSPHRASE = "testpassphrase123"
    public static final String NODE_USER_PASSWORD  = "testpassword123"
    public static final String USER_VAULT_PASSWORD = "vault123"
    public static final List<String> NODE_LIST = ["ssh-node-passphrase", "ssh-node-file-passphrase", "ssh-node-file", "ssh-node", "ssh-agent-node", "password-node"]

    def setupSpec() {
        String keyPath = getClass().getClassLoader().getResource("docker/compose/oss").getPath()+"/keys"

        loadKeysForNodes(keyPath, TEST_PROJECT, NODE_KEY_PASSPHRASE, NODE_USER_PASSWORD, USER_VAULT_PASSWORD)

        setupProjectArchiveDirectory(
                TEST_PROJECT,
                new File(getClass().getResource(TEST_SSH_ARCHIVE_DIR).getPath()),
                [
                        "importConfig": "true",
                        "importACL": "true",
                        "importNodesSources": "true",
                        "jobUuidOption": "preserve"
                ]
        )
        waitingResourceEnabled(TEST_PROJECT, "ssh-node")
    }
    def cleanupSpec(){
        deleteProject(TEST_PROJECT)
    }

    def setup() {
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
    }

    @Unroll
    def "execution succeeds on the specific nodes matching the filter (nextUi: #nextUi)"() {
        when:
        def commandPage = go CommandPage, TEST_PROJECT
        commandPage.nextUi = nextUi
        def executionShowPage = page ExecutionShowPage
        then:
        commandPage.nodeFilterTextField.click()
        commandPage.nodeFilterTextField.sendKeys("tags: executor-test")
        commandPage.filterNodeButton.click()
        commandPage.waitForElementToBeClickable commandPage.commandTextField
        commandPage.commandTextField.click()
        commandPage.waitForElementAttributeToChange commandPage.commandTextField, 'disabled', null
        commandPage.commandTextField.sendKeys "echo running test '" + this.class.name.toString() + "'"
        commandPage.runButton.click()
        def href = commandPage.runningButtonLink().getAttribute("href")
        commandPage.driver.get href + "#output"
        expect:
        executionShowPage.validatePage()
        executionShowPage.waitForElementAttributeToChange executionShowPage.executionStateDisplayLabel, 'data-execstate', 'SUCCEEDED'
        // Waits to ensure there is a log line for each node matching the executor-test tag
        waitFor({ executionShowPage.getExecLogLines() }, { it.size() == NODE_LIST.size() })
        where:
            nextUi << [false, true]
    }
}