package org.rundeck.tests.functional.selenium.nodes

import org.openqa.selenium.By
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.execution.CommandPage
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.nodes.NodesPage

@SeleniumCoreTest
class NodesSpec extends SeleniumBase {

    public static final String TEST_PROJECT = "nodes-test"

    public static final String TEST_SSH_ARCHIVE_DIR = "/projects-import/$TEST_PROJECT"
    public static final String NODE_KEY_PASSPHRASE = "testpassphrase123"
    public static final String NODE_USER_PASSWORD = "testpassword123"
    public static final String USER_VAULT_PASSWORD = "vault123"

    NodesPage nodesPage

    def setupSpec() {
        String keyPath = getClass().getClassLoader().getResource("docker/compose/oss").getPath() + "/keys"

        loadKeysForNodes(keyPath, TEST_PROJECT, NODE_KEY_PASSPHRASE, NODE_USER_PASSWORD, USER_VAULT_PASSWORD)

        setupProjectArchiveDirectory(TEST_PROJECT,
                new File(getClass().getResource(TEST_SSH_ARCHIVE_DIR).getPath()),
                ["importConfig"      : "true",
                 "importACL"         : "true",
                 "importNodesSources": "true",
                 "jobUuidOption"     : "preserve"])
        waitingResourceEnabled(TEST_PROJECT, "ssh-node")
    }

    def setup() {
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)

        nodesPage = page NodesPage
        nodesPage.goToProjectNodesPage TEST_PROJECT
    }

    def "project nodes page loads"() {
        expect:
        nodesPage.waitForUrlToContain("$TEST_PROJECT/nodes")
    }

    def "all nodes shown for the .* filter"() {
        when:
        nodesPage.setNodeInputText(".*")
        nodesPage.clickSearchNodes()
        then:
        nodesPage.waitForNumberOfElementsToBe(nodesPage.nodeListTrBy, 3)
        nodesPage.getDisplayedNodesCount() == 3
    }

    def "matching node shown when filtering by node name"() {
        given:
        nodesPage.setNodeInputText(".*")
        nodesPage.clickSearchNodes()
        when:
        nodesPage.byAndWaitClickable(NodesPage.nodesTableNodeFilterLinkByResolver("password-node")).click()
        then:
        nodesPage.waitForNumberOfElementsToBeOne(nodesPage.nodeListTrBy)
        nodesPage.getDisplayedNodesCount() == 1
        nodesPage.expectLinkTextToExist("password-node")
    }

    def "matching nodes shown when filtering by the tag name attribute"() {
        given:
        nodesPage.setNodeInputText(".*")
        nodesPage.clickSearchNodes()
        when:
        nodesPage.els(NodesPage.nodesTableNodeFilterLinkByResolver("auth-method-password")).first().click()
        then:
        nodesPage.waitForNumberOfElementsToBeOne(nodesPage.nodeListTrBy)
        nodesPage.getDisplayedNodesCount() == 1
        nodesPage.expectLinkTextToExist("password-node")
    }

    def "nodes list displays appropriate node attributes"() {
        when:
        nodesPage.setNodeInputText("name: password-node")
        nodesPage.clickSearchNodes()
        nodesPage.waitForNumberOfElementsToBeOne(nodesPage.nodeListTrBy)
        then: "node name link is shown"
        nodesPage.expectLinkTextToExist("password-node")
        then: "tag links are shown"
        nodesPage.expectLinkTextToExist("auth-method-password")
        nodesPage.partialLinkTextIsPresent("rundeck")
        nodesPage.expectLinkTextToExist("ssh-node")
    }

    def "appropriate node attributes displayed when a node is expanded"() {
        given:
        nodesPage.setNodeInputText("name: password-node")
        nodesPage.clickSearchNodes()
        nodesPage.waitForNumberOfElementsToBeOne(nodesPage.nodeListTrBy)
        when:
        nodesPage.byAndWaitClickable(By.linkText("password-node")).click()
        then: "Expected links are shown"
        nodesPage.expectLinkTextToExist("password-node")
        nodesPage.partialLinkTextIsPresent("rundeck")
        nodesPage.partialLinkTextIsPresent("unix")
        then: "Expected text fields are shown"
        nodesPage.expectPartialTextToExist("keys/project/core-jsch-executor-test/ssh-node.pass")
        nodesPage.expectPartialTextToExist("jsch-ssh")
        nodesPage.expectPartialTextToExist("jsch-scp")
        nodesPage.expectPartialTextToExist("password")
    }

    def "a nodes filter can be saved and used"() {
        given:
        final testFilterName = "test_filter_1"

        nodesPage.setNodeInputText("name: password-node")
        nodesPage.clickSearchNodes()
        nodesPage.waitForNumberOfElementsToBeOne(nodesPage.nodeListTrBy)

        when:
        nodesPage.byAndWaitClickable(nodesPage.nodeFilterInputToggleBy).click()
        nodesPage.byAndWaitClickable(nodesPage.nodeFilterInputDropdownSaveFilterBy).click()

        nodesPage.setSaveNodeFilterModalNameFieldValue(testFilterName)
        nodesPage.byAndWaitClickable(nodesPage.saveNodeFilterModalSaveButtonBy).click()

        then: "filter is saved and used right away"
        nodesPage.expectPartialTextToExist(testFilterName)
        nodesPage.waitForNumberOfElementsToBe(nodesPage.nodeListTrBy, 1)

        when:
        // Reset the filter to show all nodes
        nodesPage.byAndWaitClickable(nodesPage.nodeFilterInputToggleBy).click()
        nodesPage.byAndWaitClickable(By.partialLinkText("Show all nodes")).click()
        nodesPage.waitForNumberOfElementsToBeMoreThan(nodesPage.nodeListTrBy, 1)

        // Select the saved filter from the dropdown
        nodesPage.byAndWaitClickable(nodesPage.nodeFilterInputToggleBy).click()
        nodesPage.byAndWaitClickable(By.partialLinkText(testFilterName)).click()

        then:
        nodesPage.expectPartialTextToExist(testFilterName)
        nodesPage.waitForNumberOfElementsToBe(nodesPage.nodeListTrBy, 1)
    }

    def "a  command can be ran from the nodes filter"() {
        given:
        nodesPage.setNodeInputText("tags: \"ssh-node\"")
        nodesPage.clickSearchNodes()
        nodesPage.waitForNumberOfElementsToBe(nodesPage.nodeListTrBy, 2)

        when:
        nodesPage.byAndWaitClickable(nodesPage.actionsDropdownToggleBy).click()
        nodesPage.byAndWaitClickable(nodesPage.actionsDropdownRunCommandBy).click()

        CommandPage commandPage = page CommandPage

        then: "Ensure the filtered nodes appear on the page"
        commandPage.waitForUrlToContain("command/run")
        commandPage.byAndWaitClickable(By.partialLinkText("ssh-node"))

        commandPage.expectPartialTextToExist("2 Nodes Matched")
        commandPage.expectLinkTextToExist("ssh-node")
        commandPage.expectLinkTextToExist("password-node")
    }

    def "a  job can be created from the nodes filter"() {
        given:
        nodesPage.setNodeInputText("tags: \"ssh-node\"")
        nodesPage.clickSearchNodes()
        nodesPage.waitForNumberOfElementsToBe(nodesPage.nodeListTrBy, 2)

        when:
        nodesPage.byAndWaitClickable(nodesPage.actionsDropdownToggleBy).click()
        nodesPage.byAndWaitClickable(nodesPage.actionsDropdownSaveJobBy).click()
        JobCreatePage jobCreatePage = page JobCreatePage
        jobCreatePage.byAndWaitClickable(By.partialLinkText("Nodes")).click()

        then: "Ensure the filtered nodes appear on the page"
        jobCreatePage.waitForUrlToContain("job/create")
        jobCreatePage.byAndWaitClickable(By.partialLinkText("ssh-node"))

        jobCreatePage.expectPartialTextToExist("2 Nodes Matched")
        jobCreatePage.expectLinkTextToExist("ssh-node")
        jobCreatePage.expectLinkTextToExist("password-node")
    }
}
