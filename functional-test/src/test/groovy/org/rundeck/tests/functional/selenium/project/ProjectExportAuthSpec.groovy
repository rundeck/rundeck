package org.rundeck.tests.functional.selenium.project

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.TopMenuPage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.ProjectExportPage

@SeleniumCoreTest
class ProjectExportAuthSpec extends SeleniumBase {

    static final String PROJECT_NAME = 'ProjectExportAuthSpec'
    public static final String ACLPOLICY_FILE = PROJECT_NAME + ".aclpolicy"

    static final String USER_PASSWORD = 'password'

    def setupSpec() {
        setupProject(PROJECT_NAME)
        importSystemAcls("/${ACLPOLICY_FILE}", ACLPOLICY_FILE)
    }
    def setup(){
    }

    def cleanupSpec() {
        deleteProject(PROJECT_NAME)
        deleteSystemAcl(ACLPOLICY_FILE)
    }

    def "project access #projAuth acl access #aclAuth shows correct export options"() {

        when:
            def loginPage = go LoginPage
            loginPage.login(user, USER_PASSWORD)
            sleep(2000)
            def projectExportPage = go ProjectExportPage, PROJECT_NAME
            def boxNames = projectExportPage.checkBoxes*.getAttribute('name')
            def disabledBoxes = projectExportPage.disabledCheckboxes*.text

        then:

            verifyAll {
                boxNames.containsAll(expected)
                disabledBoxes.containsAll(expectDisabled)
            }

        cleanup:
            page TopMenuPage logOut()
            sleep(10000)
        where:
            user        | projAuth    | aclAuth       | expected       | expectDisabled
            'AuthTest1' | 'export'    | ['read']      | ['exportAcls'] | ['SCM Configuration (Unauthorized)']
            'AuthTest2' | 'export'    | ['admin']     | ['exportAcls'] | ['SCM Configuration (Unauthorized)']
            'AuthTest3' | 'export'    | ['app_admin'] | ['exportAcls'] | ['SCM Configuration (Unauthorized)']
            'AuthTest4' | 'configure' | []            | ['exportScm']  | ['ACL Policies (Unauthorized)']
            'AuthTest5' | 'admin'     | []            | ['exportScm']  | ['ACL Policies (Unauthorized)']
            'AuthTest6' | 'app_admin' | []            | ['exportScm']  | ['ACL Policies (Unauthorized)']
            'AuthTest7' | 'export'    | []            | []             | ['ACL Policies (Unauthorized)', 'SCM Configuration (Unauthorized)']
    }

}
