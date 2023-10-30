package org.rundeck.tests.functional.selenium.visual

import org.junit.Assert
import org.rundeck.tests.functional.selenium.pages.HomePage
import org.rundeck.tests.functional.selenium.pages.JobCreatePage
import org.rundeck.tests.functional.selenium.pages.JobListPage
import org.rundeck.tests.functional.selenium.pages.LoginPage
import org.rundeck.tests.functional.selenium.pages.ProjectCreatePage
import org.rundeck.tests.functional.selenium.pages.SideBarPage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.setup.NavLinkTypes

@SeleniumCoreTest
class CreateSpec extends SeleniumBase {

    def "Create Project has basic fields"() {
        when:
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
        def homePage = page HomePage
        homePage.createProjectButton()
        def projectCreatePage = page ProjectCreatePage

        then:
        currentUrl.contains("/resources/createProject")
        projectCreatePage.getProjectNameInput()
        projectCreatePage.getLabelInput()
        projectCreatePage.getDescriptionInput()
    }

    def "Create Job has basic fields"() {
        when:
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
        def homePage = go HomePage
        homePage.createProjectButton()

        def projectCreatePage = page ProjectCreatePage
        projectCreatePage.createProject(toCamelCase specificationContext.currentFeature.name)

        def sideBarPage = page SideBarPage
        sideBarPage.goTo NavLinkTypes.JOBS
        sleep 5000
        def jobListPage = page JobListPage
        jobListPage.createJobButton.click()
        def jobCreatePage = page JobCreatePage

        then:
        currentUrl.contains("/job/create")
        jobCreatePage.getJobNameInput()
        jobCreatePage.getGroupPathInput()
        jobCreatePage.getDescriptionTextarea()
    }

    def "Create Job invalid empty name"() {


        when:
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
        def homePage = go HomePage
        homePage.createProjectButton()

        def projectCreatePage = page ProjectCreatePage
        projectCreatePage.createProject(toCamelCase specificationContext.currentFeature.name)

        def sideBarPage = page SideBarPage
        sideBarPage.goTo NavLinkTypes.JOBS
        sleep 5000
        def jobListPage = page JobListPage
        jobListPage.createJobButton.click()
        JobCreatePage jobCreatePage = page JobCreatePage

        then:
        jobCreatePage.setJobNameInput("")
        jobCreatePage.clickSaveBtn()
        jobCreatePage.errorAlert()
        jobCreatePage.formValidationAlert()
        String text = jobCreatePage.formValidationAlert()
        text.equals('"Job Name" parameter cannot be blank')

        text.equals('Workflow must have at least one step')

      //  jobCreatePage.formValidationAlert.getText()== '"Job Name" parameter cannot be blank'
       // jobShowPage.notificationDefinition.getText() == 'Workflow must have at least one step'
/*

        ///
        let jobName=await jobCreatePage.jobNameInput()
        await jobName.clear()
        let save = await jobCreatePage.saveButton()
        await save.click()

        await ctx.driver.wait(until.titleMatches(/.*(Create New Job).*$/i), 5000)
        //verify error messages
        let error = await jobCreatePage.errorAlert()
        await expect(error.getText()).resolves.toContain('Error saving Job')

        //verify validation message
        let validation = await jobCreatePage.formValidationAlert()
        let text = await validation.getText()
        expect(text).toContain('"Job Name" parameter cannot be blank')
        expect(text).toContain('Workflow must have at least one step')
 */

        /*
        it('invalid empty name', async () => {
            await jobCreatePage.get()
            await ctx.driver.wait(until.urlContains('/job/create'), 25000)
            let jobName=await jobCreatePage.jobNameInput()
            await jobName.clear()
            let save = await jobCreatePage.saveButton()
            await save.click()

            await ctx.driver.wait(until.titleMatches(/.*(Create New Job).*$/i), 5000)
            //verify error messages
            let error = await jobCreatePage.errorAlert()
            await expect(error.getText()).resolves.toContain('Error saving Job')

            //verify validation message
            let validation = await jobCreatePage.formValidationAlert()
            let text = await validation.getText()
            expect(text).toContain('"Job Name" parameter cannot be blank')
            expect(text).toContain('Workflow must have at least one step')
        })  */





    }




    def "invalid empty workflow"(){

        when:
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
        def homePage = go HomePage
        homePage.createProjectButton()

        def projectCreatePage = page ProjectCreatePage
        projectCreatePage.createProject(toCamelCase specificationContext.currentFeature.name)

        def sideBarPage = page SideBarPage
        sideBarPage.goTo NavLinkTypes.JOBS
        sleep 5000
        def jobListPage = page JobListPage
        jobListPage.createJobButton.click()
        JobCreatePage jobCreatePage = page JobCreatePage

        then:
        jobCreatePage.setJobNameInput("a job")
        jobCreatePage.clickSaveBtn()
        jobCreatePage.formValidationAlert()
        var text = jobCreatePage.formValidationAlert()

        Assert.assertFalse(text.contains('"Job Name" parameter cannot be blank'))

        text.equals('Workflow must have at least one step')










    }

    /*
      it('invalid empty workflow', async () => {
        await jobCreatePage.get()
        await ctx.driver.wait(until.urlContains('/job/create'), 25000)
        let jobName=await jobCreatePage.jobNameInput()
        await jobName.sendKeys('a job')
        let save = await jobCreatePage.saveButton()
        await save.click()

        await ctx.driver.wait(until.titleMatches(/.*(Create New Job).*$/i), 5000)
        //verify error messages
        let error = await jobCreatePage.errorAlert()
        await expect(error.getText()).resolves.toContain('Error saving Job')

        //verify validation message
        let validation = await jobCreatePage.formValidationAlert()
        let text = await validation.getText()
        expect(text).not.toContain('"Job Name" parameter cannot be blank')
        expect(text).toContain('Workflow must have at least one step')
    })
     */



















}