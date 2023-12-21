package org.rundeck.tests.functional.selenium

import org.rundeck.tests.functional.selenium.pages.EditNodesFilePage
import org.rundeck.tests.functional.selenium.pages.EditNodesPage
import org.rundeck.tests.functional.selenium.pages.ProjectListPage
import org.rundeck.tests.functional.selenium.pages.login.LoginPage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase

import java.util.stream.Collectors

@SeleniumCoreTest
class AceEditorSpec extends SeleniumBase{

    private static def projectName = 'resourcesTest'
    private static def jsonFileIndex = 2

    def setupSpec(){
        setupProjectWithNodes(projectName, "/projects-import/resourcesTest.zip")
    }

    def "Edit json file resource model with indented text"(){

        // 1. Upload a project with a (json) file resource model created
        // 2. Attempt to modify the file (Ace Editor rendered into view)
        // Asserts the JSON is indented.

        setup:
        LoginPage loginPage = page LoginPage
        EditNodesPage editNodesPage = page EditNodesPage
        editNodesPage.setProject(projectName)
        EditNodesFilePage editNodesFilePage = page EditNodesFilePage
        editNodesFilePage.setProject(projectName)
        editNodesFilePage.setIndex(jsonFileIndex)

        when:
        loginPage.go()
        loginPage.login(TEST_USER, TEST_PASS)
        page(ProjectListPage)
        editNodesPage.go()
        editNodesFilePage.go()
        editNodesFilePage.waitForAceToRender()
        def linesInAceGutter = editNodesFilePage.aceGutterElement().getText()
        List<String> linesAsList = Arrays.stream(linesInAceGutter.split("\\n"))
                .collect(Collectors.toList());

        then:
        linesAsList.size() > 1

    }

}
