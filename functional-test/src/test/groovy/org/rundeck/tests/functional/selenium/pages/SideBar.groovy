package org.rundeck.tests.functional.selenium.pages

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.container.SeleniumContext

import java.time.Duration

@CompileStatic
class SideBar extends BasePage {

    SideBar(final SeleniumContext context) {
        super(context)
    }

    @Override
    String getLoadPath() {
        return null
    }

    WebElement goTo(SideBarNavLinks sideBarLink){
        new WebDriverWait(driver, Duration.ofSeconds(5)).until(ExpectedConditions.numberOfElementsToBe(By.id(sideBarLink.getId()), 1))
        el By.id(sideBarLink.getId())
    }
}

enum SideBarNavLinks {

    JOBS("nav-jobs-link", "jobs", false)

    private String id
    private String urlToBe
    private boolean isUnderProjectConfig

    SideBarNavLinks(String id, String urlToBe, boolean isUnderProjectConfig) {
        this.id = id
        this.urlToBe = urlToBe
        this.isUnderProjectConfig = isUnderProjectConfig
    }

    String getId() { return id}
    String getUrlToBe() { return urlToBe}
    boolean getIsUnderProjectConfig() { return isUnderProjectConfig}

}
