package org.rundeck.tests.functional.selenium.project

import org.openqa.selenium.By
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.common.navigation.NavLinkTypes
import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.DashboardPage
import org.rundeck.util.gui.pages.project.SideBarPage
import spock.lang.Unroll


@SeleniumCoreTest
class NavSidebarSpec extends SeleniumBase {
    static final String SPEC_PROJECT = "NavSidebarSpec"

    static final String ADMIN_USER = "NavSidebarSpec1"
    static final String READONLY_USER = "NavSidebarSpec2"
    static final String DELETE_USER = "NavSidebarSpec3"
    static final String EXPORT_USER = "NavSidebarSpec4"
    static final String IMPORT_USER = "NavSidebarSpec5"
    static final String CONFIGURE_USER = "NavSidebarSpec6"
    static final String ACL_READ_USER = "NavSidebarSpec7"
    static final String ACL_ADMIN_USER = "NavSidebarSpec8"
    static final String APP_ADMIN_USER = "NavSidebarSpec9"
    static final String USER_PASS = "password"

    def setupSpec() {
        setupProject(SPEC_PROJECT)
        //import acls
        importSystemAcls("/NavSidebarSpec.aclpolicy", "NavSidebarSpec.aclpolicy")
    }

    def cleanupSpec() {
        deleteProject(SPEC_PROJECT)
        deleteSystemAcl("NavSidebarSpec.aclpolicy")
    }

    @Override
    String toString() {
        return super.toString()
    }

    /**
     * All project settings navlinks
     */
    static List<NavLinkTypes> PROJ_SETTINGS_NAVLINKS = [
        NavLinkTypes.EDIT_NODES,
        NavLinkTypes.MOTD,
        NavLinkTypes.README,
        NavLinkTypes.PROJECT_CONFIG,
        NavLinkTypes.ACCESS_CONTROL,
        NavLinkTypes.SETUP_SCM,
        NavLinkTypes.EXPORT_ARCHIVE,
        NavLinkTypes.IMPORT_ARCHIVE,
        NavLinkTypes.DELETE_PROJECT,
    ]
    /**
     * Nav links visible with "configure" authorization
     */
    static List<NavLinkTypes> PROJ_SETTINGS_CONFIGURE_NAVLINKS = [
        NavLinkTypes.PROJECT_CONFIG,
        NavLinkTypes.STORAGE,
        NavLinkTypes.EDIT_NODES,
        NavLinkTypes.README,
        NavLinkTypes.MOTD,
        NavLinkTypes.SETUP_SCM
    ]

    @Unroll
    def "User #username with specific auth sees specific navlinks #visible"() {
        given:
            LoginPage loginPage = page LoginPage
            HomePage homePage = page HomePage
            SideBarPage sideBarPage = page SideBarPage
        when:
            loginPage.go()
            loginPage.login(username, USER_PASS)
            homePage.validatePage()
            homePage.goProjectHome(SPEC_PROJECT)
            sideBarPage.projectSettingsField.click()
            sideBarPage.waitForNavVisible()

        then:
            verifyAll {
                for (NavLinkTypes navItem : PROJ_SETTINGS_NAVLINKS) {
                    if(navItem in visible){
                        assert sideBarPage.els(By.id(navItem.id)).size()==1
                        assert sideBarPage.els(By.id(navItem.id)).every{it.displayed}
                    }
                    else {
                        assert !sideBarPage.els(By.id(navItem.id))
                    }
                }
            }
        where:
            username       | visible
            ADMIN_USER     | (PROJ_SETTINGS_NAVLINKS - NavLinkTypes.ACCESS_CONTROL)
            APP_ADMIN_USER     | (PROJ_SETTINGS_NAVLINKS - NavLinkTypes.ACCESS_CONTROL)
            DELETE_USER    | [NavLinkTypes.DELETE_PROJECT]
            EXPORT_USER    | [NavLinkTypes.EXPORT_ARCHIVE]
            IMPORT_USER    | [NavLinkTypes.IMPORT_ARCHIVE]
            CONFIGURE_USER | PROJ_SETTINGS_CONFIGURE_NAVLINKS
            ACL_READ_USER  | [NavLinkTypes.ACCESS_CONTROL]
            ACL_ADMIN_USER | [NavLinkTypes.ACCESS_CONTROL]
    }
    @Unroll
    def "User with readonly access does not see project settings nav or items"() {
        given:
            LoginPage loginPage = page LoginPage
            HomePage homePage = page HomePage
            SideBarPage sideBarPage = page SideBarPage
        when:
            loginPage.go()
            loginPage.login(READONLY_USER, USER_PASS)
            homePage.validatePage()
            homePage.goProjectHome(SPEC_PROJECT)

        then: "project settings button is not shown, or the menu does not contain any of the core nav links"
            if(!sideBarPage.els(sideBarPage.projectSettings)){
                //Project Settings is not shown
                assert !sideBarPage.els(sideBarPage.projectSettings)
            }else{
                //Project Settings is shown when other link is present, but the configuration links should not be present
                verifyAll {
                    for (NavLinkTypes navItem : PROJ_SETTINGS_NAVLINKS) {
                        assert !sideBarPage.els(By.id(navItem.id))
                    }
                }
            }

    }
}
