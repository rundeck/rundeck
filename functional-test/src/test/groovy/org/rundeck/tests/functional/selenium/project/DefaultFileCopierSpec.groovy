package org.rundeck.tests.functional.selenium.project

import org.openqa.selenium.WebElement
import org.rundeck.util.annotations.ExcludePro
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.common.navigation.NavProjectSettings
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.ProjectEditPage

/**
 * Sets properties for file copier plugin and validates if the props get saved in a project.
 *
 */
@SeleniumCoreTest
class DefaultFileCopierSpec extends SeleniumBase{

    private static final List<String> availableFileCopiers = Arrays.asList(new String[] { "Local", "SCP", "SSHJ-SCP", "Stub", "Script Execution", "Ansible File Copier", "WinRM Python File Copier", "openssh / file-copier" });

    /**
     * This is excluded for PRO since it has a different list
     */
    @ExcludePro
    def "check file copier list"(){
        given:
        def projectName = "fileCopierList"
        setupProject(projectName)
        ProjectEditPage editPage = page ProjectEditPage, projectName
        when:
        go(LoginPage).login(TEST_USER, TEST_PASS)
        editPage.go()
        editPage.clickNavLink(NavProjectSettings.DEFAULT_FILE_COPIER)
        editPage.clickFileCopierDropDown()
        then:
        editPage.getFileCopierList().size() == availableFileCopiers.size()
        for(WebElement fileCopier : editPage.getFileCopierList()){
            availableFileCopiers.contains(fileCopier.getText())
        }
        cleanup:
        deleteProject(projectName)
    }

    def "scp file copier config"(){
        given:
        def projectName = "scpFileCopier"
        setupProject(projectName)
        ProjectEditPage editPage = page ProjectEditPage
        editPage.loadProjectEditForProject(projectName)
        def fileCopierConfigMap = ["keypath":"/ssh/key/file/path",
                                   "keystoragepath":"keys/example/exampleKey.key",
                                   "passwordstoragepath":"keys/example/exampleKey.key",
                                   "passphrasestoragepath":"keys/example/exampleKey.key",
                                   "ssh-bind-address":"bindAddress"]
        def selectableProps = [
                "authentication":"privateKey",
        ]
        when:
        go(LoginPage).login(TEST_USER, TEST_PASS)
        editPage.go()
        editPage.clickNavLink(NavProjectSettings.DEFAULT_FILE_COPIER)
        editPage.clickFileCopierDropDown()
        editPage.selectFileCopier(ProjectEditPage.FileCopierEnum.SCP)
        editPage.setFileCopierValues(fileCopierConfigMap, null, selectableProps)
        editPage.save()
        editPage.go()
        editPage.clickNavLink(NavProjectSettings.DEFAULT_FILE_COPIER)
        def defaultFileCopierConfigInGui = editPage.getFileCopierConfig(
                fileCopierConfigMap
        )
        then:
        defaultFileCopierConfigInGui == fileCopierConfigMap
        cleanup:
        deleteProject(projectName)
    }

    def "Sshj SCP File copier config"(){
        given:
        def projectName = "sshjScpFileCopier"
        setupProject(projectName)
        ProjectEditPage editPage = page ProjectEditPage
        editPage.loadProjectEditForProject(projectName)
        def fileCopierConfigMap = [
                "retryCounter"         : "3",
                "keypath"              : "/ssh/key/file/path",
                "keystoragepath"       : "keys/example/exampleKey.key",
                "passwordstoragepath"  : "keys/example/exampleKey.key",
                "passphrasestoragepath": "keys/example/exampleKey.key",
                "keepAliveInterval"     : "30"
        ]
        def selectableProps = [
                "authentication"       : "privateKey",
        ]
        when:
        go(LoginPage).login(TEST_USER, TEST_PASS)
        editPage.go()
        editPage.clickNavLink(NavProjectSettings.DEFAULT_FILE_COPIER)
        editPage.clickFileCopierDropDown()
        editPage.selectFileCopier(ProjectEditPage.FileCopierEnum.SSHJ_SCP)
        editPage.setFileCopierValues(fileCopierConfigMap, null, selectableProps)
        editPage.save()
        editPage.go()
        editPage.clickNavLink(NavProjectSettings.DEFAULT_FILE_COPIER)
        def defaultFileCopierConfigInGui = editPage.getFileCopierConfig(fileCopierConfigMap)
        then:
        defaultFileCopierConfigInGui == fileCopierConfigMap
        cleanup:
        deleteProject(projectName)
    }

    def "Stub file copier config"(){
        given:
        def projectName = "StubFileCopier"
        setupProject(projectName)
        ProjectEditPage editPage = page ProjectEditPage
        editPage.loadProjectEditForProject(projectName)
        when:
        go(LoginPage).login(TEST_USER, TEST_PASS)
        editPage.go()
        editPage.clickNavLink(NavProjectSettings.DEFAULT_FILE_COPIER)
        editPage.clickFileCopierDropDown()
        editPage.selectFileCopier(ProjectEditPage.FileCopierEnum.STUB)
        editPage.save()
        editPage.go()
        editPage.clickNavLink(NavProjectSettings.DEFAULT_FILE_COPIER)

        then:
        editPage.selectedDefaultFileCopier.text.contains("Stub")

        cleanup:
        deleteProject(projectName)
    }

    def "Script file copier config"(){
        given:
        def projectName = "ScriptFileCopier"
        setupProject(projectName)
        ProjectEditPage editPage = page ProjectEditPage
        editPage.loadProjectEditForProject(projectName)
        def fileCopierConfigMap = [
                "command"    : "customCommand",
                "filepath"   : "/file/path",
                "interpreter": "interpreter",
                "directory"  : "custom/dir"
        ]
        when:
        go(LoginPage).login(TEST_USER, TEST_PASS)
        editPage.go()
        editPage.clickNavLink(NavProjectSettings.DEFAULT_FILE_COPIER)
        editPage.clickFileCopierDropDown()
        editPage.selectFileCopier(ProjectEditPage.FileCopierEnum.SCRIPT_EXECUTION)
        editPage.setFileCopierValues(fileCopierConfigMap)
        editPage.save()
        editPage.go()
        editPage.clickNavLink(NavProjectSettings.DEFAULT_FILE_COPIER)
        def defaultFileCopierConfigInGui = editPage.getFileCopierConfig(fileCopierConfigMap)
        then:
        defaultFileCopierConfigInGui == fileCopierConfigMap
        cleanup:
        deleteProject(projectName)
    }

    def "WinRM file copier config"(){
        given:
        def projectName = "WinRmFileCopier"
        setupProject(projectName)
        ProjectEditPage editPage = page ProjectEditPage
        editPage.loadProjectEditForProject(projectName)
        def fileCopierConfigMap = [
                "winrmport"    : "1111",
                "krb5config"   : "/etc/krb.conf",
                "kinit": "kinito",
                "username": "rundeck",
                "password_storage_path": "keys/example/exampleKey.key",
                "certpath" : "/tmp/certPath",
        ]
        def clickableProps = [
                "debug",
                "disabletls12",
                "krbdelegation"
        ]
        def selectableProps = [
                "nossl" : "true",
                "authtype"  : "kerberos",
                "winrmtransport": "https"
        ]
        when:
        go(LoginPage).login(TEST_USER, TEST_PASS)
        editPage.go()
        editPage.clickNavLink(NavProjectSettings.DEFAULT_FILE_COPIER)
        editPage.clickFileCopierDropDown()
        editPage.selectFileCopier(ProjectEditPage.FileCopierEnum.WINRM_FILE_COPIER)
        editPage.setFileCopierValues(fileCopierConfigMap, clickableProps, selectableProps)
        editPage.save()
        editPage.go()
        editPage.clickNavLink(NavProjectSettings.DEFAULT_FILE_COPIER)
        def expectedOutcome = [:]
        expectedOutcome << fileCopierConfigMap
        clickableProps.each {
            expectedOutcome.put(it, "true")
        }
        expectedOutcome << selectableProps
        def defaultFileCopierConfigInGui = editPage.getFileCopierConfig(
                fileCopierConfigMap,
                clickableProps,
                selectableProps
        )
        then:
        defaultFileCopierConfigInGui == expectedOutcome
        cleanup:
        deleteProject(projectName)
    }

    def "Open Ssh file copier config"(){
        given:
        def projectName = "OpenSshFileCopier"
        setupProject(projectName)
        ProjectEditPage editPage = page ProjectEditPage
        editPage.loadProjectEditForProject(projectName)
        def fileCopierConfigMap = [
                "ssh_key_storage_path":"keys/example/exampleKey.key",
                "ssh_key_passphrase_storage_path":"keys/example/exampleKey.key",
                "ssh_password_storage_path":"keys/example/exampleKey.key",
                "ssh_options":"-o ConnectTimeout=10",
                "ssh_password_option":"option.password",
                "ssh_key_passphrase_option":"option.passphrase"
        ]
        def selectableProps = [
                "authentication":"privatekey"
        ]
        when:
        go(LoginPage).login(TEST_USER, TEST_PASS)
        editPage.go()
        editPage.clickNavLink(NavProjectSettings.DEFAULT_FILE_COPIER)
        editPage.clickFileCopierDropDown()
        editPage.selectFileCopier(ProjectEditPage.FileCopierEnum.OPENSSH)
        editPage.setFileCopierValues(fileCopierConfigMap, null, selectableProps)
        editPage.save()
        editPage.go()
        editPage.clickNavLink(NavProjectSettings.DEFAULT_FILE_COPIER)
        def expectedOutcome = [:]
        expectedOutcome << fileCopierConfigMap
        expectedOutcome << selectableProps
        def defaultFileCopierConfigInGui = editPage.getFileCopierConfig(
                fileCopierConfigMap,
                null,
                selectableProps
        )
        then:
        defaultFileCopierConfigInGui == expectedOutcome
        cleanup:
        deleteProject(projectName)
    }
}
