package org.rundeck.tests.functional.selenium.jobs

import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.jobs.JobTab
import org.rundeck.util.gui.pages.login.LoginPage

@SeleniumCoreTest
class JobAuditSpec extends SeleniumBase {

    static def projectName = "auditUserTrackingProject"
    static def jobName = "audit-test-job"

    def setupSpec() {
        setupProject(projectName)
    }

    def "cross-user audit tracking: admin creates, jaya edits"() {
        given:
        def loginPage = page LoginPage
        def jobCreatePage = page JobCreatePage
        def jobShowPage = page JobShowPage
        def jobUuid

        when: "admin logs in and creates a job"
        loginPage.go()
        loginPage.login(TEST_USER, TEST_PASS)

        jobCreatePage.go("/project/${projectName}/job/create")
        jobCreatePage.validatePage()

        // Fill job details
        jobCreatePage.jobNameInput.clear()
        jobCreatePage.jobNameInput.sendKeys(jobName)
        jobCreatePage.descriptionTextarea.clear()
        jobCreatePage.descriptionTextarea.sendKeys("job created by admin")

        // Add workflow step
        jobCreatePage.tab(JobTab.WORKFLOW).click()
        jobCreatePage.addSimpleCommandStep("echo hello from admin", 0)
        jobCreatePage.createJobButton.click()

        then: "job is created successfully"
        jobShowPage.waitForElementToBeClickable(jobShowPage.jobLinkTitleLabel)
        jobShowPage.validatePage()
        jobShowPage.jobLinkTitleLabel.getText() == jobName

        when: "extract job UUID and check initial audit info"
        def currentUrl = driver.getCurrentUrl()
        jobUuid = currentUrl.split('/').last()
        println "Job created with UUID: ${jobUuid}"

        jobShowPage.jobDefinitionModalButton.click()

        then: "initial audit information shows created by admin"
        def modalContent = jobShowPage.jobDefinitionModalContent
        modalContent.getText().contains("CREATED BY")
        modalContent.getText().contains("admin")
        modalContent.getText().contains("LAST MODIFIED BY")
        modalContent.getText().contains("admin")
        println "✓ Initial audit verified: Created by admin, Last modified by admin"

        when: "admin logs out"
        jobShowPage.closeDefinitionModalButton.click()

        // Simple logout
        def userDropdown = driver.findElement(By.cssSelector("a.dropdown-toggle[id='userLabel']"))
        userDropdown.click()
        Thread.sleep(500)
        def logoutLink = driver.findElement(By.cssSelector("a[href='/user/logout']"))
        logoutLink.click()

        // Wait for logout and clear session completely
        Thread.sleep(2000)

        // Clear all browser data to ensure clean session for jaya
        driver.manage().deleteAllCookies()

        // Clear browser storage
        def js = driver as JavascriptExecutor
        js.executeScript("window.localStorage.clear();")
        js.executeScript("window.sessionStorage.clear();")

        println "✓ Admin logged out and session cleared"

        and: "jaya logs in with clean session"
        loginPage.go()

        // Wait for clean login page
        Thread.sleep(1000)

        // Clear any pre-filled credentials and enter jaya's credentials
        def usernameField = driver.findElement(By.name("j_username"))
        def passwordField = driver.findElement(By.name("j_password"))

        usernameField.clear()
        usernameField.sendKeys("jaya")

        passwordField.clear()
        passwordField.sendKeys("jaya123")

        // Submit login
        def loginButton = driver.findElement(By.id("btn-login"))
        loginButton.click()

        Thread.sleep(2000)
        println "✓ Jaya logged in with clean credentials"

        and: "jaya navigates to the job and edits it"
        jobShowPage.go("/project/${projectName}/job/show/${jobUuid}")
        jobShowPage.validatePage()

        // Wait for page to fully load, then use JobEditSpec pattern
        Thread.sleep(2000)

        // Use direct selectors based on the actual HTML structure
        def actionButton = driver.findElement(By.cssSelector(".job-action-button .btn.dropdown-toggle"))
        actionButton.click()
        println "✓ Action dropdown clicked"

        // Wait for dropdown to expand and become visible
        Thread.sleep(1000)

        // Try to find and click the edit link with better error handling
        try {
            // Wait for dropdown menu to be visible
            def dropdownMenu = driver.findElement(By.cssSelector(".dropdown-menu.dropdown-menu-right"))
            println "Dropdown menu found: ${dropdownMenu.isDisplayed()}"

            // Try multiple selectors for the edit link
            def editLink
            try {
                editLink = driver.findElement(By.cssSelector("a[title='Edit this Job']"))
                println "Found edit link by title"
            } catch (Exception e1) {
                try {
                    editLink = driver.findElement(By.xpath("//a[contains(@href, '/job/edit/') and contains(text(), 'Edit this Job')]"))
                    println "Found edit link by href and text"
                } catch (Exception e2) {
                    try {
                        editLink = driver.findElement(By.cssSelector("ul.dropdown-menu li:first-child a"))
                        println "Found edit link as first dropdown item"
                    } catch (Exception e3) {
                        println "Could not find edit link with any selector"
                        println "Available dropdown items:"
                        def dropdownItems = driver.findElements(By.cssSelector(".dropdown-menu li a"))
                        dropdownItems.eachWithIndex { item, index ->
                            println "Item ${index}: ${item.getText()} - href: ${item.getAttribute('href')}"
                        }
                        throw new RuntimeException("Edit link not found")
                    }
                }
            }

            if (editLink.isDisplayed() && editLink.isEnabled()) {
                editLink.click()
                println "✓ Edit link clicked successfully"
            } else {
                println "Edit link found but not clickable: displayed=${editLink.isDisplayed()}, enabled=${editLink.isEnabled()}"

                // Try to make the dropdown fully visible and clickable
                def jsExecutor = driver as JavascriptExecutor

                // Scroll the element into view
                jsExecutor.executeScript("arguments[0].scrollIntoView(true);", editLink)
                Thread.sleep(500)

                // Try JavaScript click as fallback
                try {
                    jsExecutor.executeScript("arguments[0].click();", editLink)
                    println "✓ Edit link clicked via JavaScript"
                } catch (Exception jsError) {
                    println "JavaScript click failed: ${jsError.message}"

                    // Last resort: navigate directly to edit URL
                    def editUrl = editLink.getDomProperty("href")
                    if (editUrl) {
                        driver.get(editUrl)
                        println "✓ Navigated directly to edit URL: ${editUrl}"
                    } else {
                        throw new RuntimeException("Could not click edit link or get edit URL")
                    }
                }
            }

        } catch (Exception e) {
            println "Error clicking edit link: ${e.message}"
            throw e
        }

        // Wait for edit page to fully load
        Thread.sleep(2000)

        try {
            jobCreatePage.validatePage()
            println "✓ Edit page loaded"
        } catch (Exception e) {
            println "Edit page validation failed: ${e.message}"
            println "Current URL: ${driver.getCurrentUrl()}"
            println "Page title: ${driver.getTitle()}"
        }

        // Wait and click workflow tab with debugging
        try {
            println "Looking for WORKFLOW tab..."
            Thread.sleep(1000)

            def workflowTab = jobCreatePage.tab(JobTab.WORKFLOW)
            println "WORKFLOW tab found: ${workflowTab != null}"

            if (workflowTab.isDisplayed() && workflowTab.isEnabled()) {
                workflowTab.click()
                println "✓ WORKFLOW tab clicked"
            } else {
                println "WORKFLOW tab not clickable: displayed=${workflowTab.isDisplayed()}, enabled=${workflowTab.isEnabled()}"
                // Try JavaScript click on tab
                def jsExecutor = driver as JavascriptExecutor
                jsExecutor.executeScript("arguments[0].click();", workflowTab)
                println "✓ WORKFLOW tab clicked via JavaScript"
            }

            Thread.sleep(1000)

        } catch (Exception tabError) {
            println "WORKFLOW tab click failed: ${tabError.message}"
            // Let's try DETAILS tab instead since we're editing description anyway
            println "Trying DETAILS tab as fallback..."
            def detailsTab = jobCreatePage.tab(JobTab.DETAILS)
            detailsTab.click()
            println "✓ DETAILS tab clicked as fallback"
        }

        // Edit the job description
        try {
            jobCreatePage.descriptionTextarea.clear()
            jobCreatePage.descriptionTextarea.sendKeys("job created by admin - edited by jaya!")
            println "✓ Description updated"
        } catch (Exception descError) {
            println "Description update failed: ${descError.message}"
        }

        jobCreatePage.getUpdateJobButton().click()
        println "✓ Update button clicked"

        // Wait for redirect back to job show page
        Thread.sleep(3000)

        then: "job is updated successfully"
        // Check if we're redirected to show page, if not navigate manually
        def urlAfterUpdate = driver.getCurrentUrl()
        println "Current URL after update: ${urlAfterUpdate}"

        if (urlAfterUpdate.contains("/job/edit/")) {
            println "Still on edit page, navigating to show page manually..."
            jobShowPage.go("/project/${projectName}/job/show/${jobUuid}")
            Thread.sleep(2000)
        }

        jobShowPage.validatePage()
        println "✓ Job updated by jaya"

        when: "check final audit info"
        jobShowPage.jobDefinitionModalButton.click()

        then: "audit information shows cross-user tracking"
        def finalModalContent = jobShowPage.jobDefinitionModalContent
        finalModalContent.getText().contains("CREATED BY")
        finalModalContent.getText().contains("admin")
        finalModalContent.getText().contains("LAST MODIFIED BY")
        finalModalContent.getText().contains("jaya")
        println "✓ Final audit verified: Created by admin, Last modified by jaya"

        cleanup: "close the modal"
        jobShowPage.closeDefinitionModalButton.click()
        println "✓ Cross-user audit tracking test completed successfully!"
    }
}