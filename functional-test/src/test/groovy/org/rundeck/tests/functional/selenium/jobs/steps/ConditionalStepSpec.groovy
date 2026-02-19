package org.rundeck.tests.functional.selenium.jobs.steps

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.jobs.JobTab
import org.rundeck.util.gui.pages.login.LoginPage

/**
 * Tests for Conditional Logic step functionality (RUN-3942)
 *
 * Tests the EA (Enhanced Architecture) mode with inline step editing,
 * nested substep management, and conditional logic configuration.
 */
@SeleniumCoreTest
class ConditionalStepSpec extends SeleniumBase {

    def setupSpec() {
        // Enable EA mode feature flag at system level
        def config = [
                [
                        "key": "rundeck.feature.earlyAccessJobConditional.enabled",
                        "value": "true",
                        "strata": "default"
                ]
        ]
        client.post("/config/save", config, Map)
        waitFeatureFlag("rundeck.feature.earlyAccessJobConditional.enabled")
    }

    def setup() {
        go(LoginPage).login(TEST_USER, TEST_PASS)
    }

    /**
     * Test #40 from E2E Test Plan: Add and edit substeps within conditional logic step
     *
     * This is a CRITICAL GAP test that validates:
     * - Nested step management within conditional steps
     * - State management across editingStepId, nestedStepToEdit
     * - Inline editing of substeps
     * - Saving conditional steps with nested commands
     *
     * E2E Reference: E2E_TEST_PLAN.md #40
     * Code Review Risk: State Management Complexity (HIGH)
     * Priority: P0 - BLOCKS MERGE
     */
    def "add and edit substeps within conditional logic step"() {
        given: "project created"
            String projectName = "ConditionalTest-${UUID.randomUUID()}"
            setupProject(projectName)

        and: "navigate to job create page with nextUi enabled"
            JobCreatePage jobCreatePage = page(JobCreatePage, projectName)
            jobCreatePage.go()

            // Enable nextUi cookie for EA mode
            jobCreatePage.driver.manage().addCookie(
                new org.openqa.selenium.Cookie("nextUi", "true", "/", null)
            )

            // Reload page to apply cookie
            jobCreatePage.go()
            jobCreatePage.tab(JobTab.WORKFLOW).click()

        when: "add step button clicked"
            jobCreatePage.clickAddStepButtonEA()
            jobCreatePage.waitForEAModal()

        and: "select Node Conditional Logic step"
            jobCreatePage.selectStepTypeEA("Conditional Logic Node Step")

        then: "EditConditionalStepCard displays inline"
            jobCreatePage.waitForEditConditionalStepCard()

        when: "configure conditional step name and condition"
            jobCreatePage.setEAStepName("Check Apache Status")
            jobCreatePage.configureCondition("node.hostname", "Contains", "web")

        and: "add first substep (Command)"
            jobCreatePage.clickAddSubstep()
            jobCreatePage.waitForEAModal()
            jobCreatePage.selectStepTypeEA("Command")
            jobCreatePage.waitForEditStepCard()

        and: "configure first substep command"
            jobCreatePage.setEACommand("systemctl status apache2")
            jobCreatePage.clickSaveSubstep()

        then: "first substep appears in substeps list"
            jobCreatePage.waitForSubstepText("systemctl status apache2")

        when: "add second substep (Script)"
            jobCreatePage.clickAddSubstep()
            jobCreatePage.waitForEAModal()
            jobCreatePage.selectStepTypeEA("Script")
            jobCreatePage.waitForEditStepCard()

        and: "configure script substep"
            jobCreatePage.setEAScript("echo Service checked")
            jobCreatePage.clickSaveSubstep()

        then: "both substeps visible in list"
            jobCreatePage.waitForSubstepCount(2)

        when: "edit first substep"
            jobCreatePage.clickEditSubstep(1)
            jobCreatePage.waitForEditStepCard()

        and: "modify command"
            jobCreatePage.setEACommand("systemctl status httpd", true)
            jobCreatePage.clickSaveSubstep()

        then: "substep updates correctly"
            jobCreatePage.waitForSubstepText("systemctl status httpd")

        when: "save conditional step"
            jobCreatePage.clickSaveConditionalStep()

        then: "conditional step appears in workflow"
            jobCreatePage.waitForConditionalStepInWorkflow("Check Apache Status")

        and: "workflow contains the conditional step"
            def workflowSteps = jobCreatePage.getWorkflowStepElements()
            assert workflowSteps.size() >= 1

        cleanup:
            deleteProject(projectName)
    }
}
