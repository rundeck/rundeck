import {Context} from '@rundeck/testdeck/context'
import {CreateContext} from '@rundeck/testdeck/test/selenium'
import {ProjectCreatePage} from 'pages/projectCreate.page'
import {LoginPage} from 'pages/login.page'
import {JobCreatePage} from 'pages/jobCreate.page'
import {JobShowPage} from "pages/jobShow.page"
import {until, By, Key} from 'selenium-webdriver'
import '@rundeck/testdeck/test/rundeck'
import {sleep} from "@rundeck/testdeck/async/util"

// We will initialize and cleanup in the before/after methods
let ctx = CreateContext({projects: ['SeleniumBasic']})
let loginPage: LoginPage
let jobCreatePage: JobCreatePage

beforeAll( async () => {
    loginPage = new LoginPage(ctx)
    jobCreatePage = new JobCreatePage(ctx, 'SeleniumBasic')
    sleep(5000)

})

beforeAll(async () => {
    await loginPage.login('admin', 'admin')
})

describe('job', () => {

    it('job workflow step context variables autocomplete', async () => {
        await jobCreatePage.get()
        await ctx.driver.wait(until.urlContains('/job/create'), 25000)
        let jobNameText='a job with workflow step context autocomplete'
        let jobName=await jobCreatePage.jobNameInput()
        await jobName.sendKeys(jobNameText)

        //add workflow step
        let wfTab=await jobCreatePage.tabWorkflow()
        await wfTab.click()
        
        let addWfStepPlugin=await jobCreatePage.addNewWfStepPlugin('Ansible Playbook Inline Workflow Node Step')

        //click add Plugin step, and wait until input fields are loaded
        await addWfStepPlugin.click()
        await jobCreatePage.waitWfStepStepForm()

        // fill remoteUrl input section
        await jobCreatePage.pluginStepConfigFillPropText("${job.id")


        let autocomplete = await jobCreatePage.findJobContextAutocomplete()

    
        await autocomplete.click()

    
        let wfStep0SaveButton=await jobCreatePage.wfStepSaveButton('0')

        //click step Save button and wait for the step content to display
        await wfStep0SaveButton.click()
        await jobCreatePage.waitWfstepvis('0')

        //save the job
        let save = await jobCreatePage.saveButton()
        await save.click()

        await ctx.driver.wait(until.urlContains('/job/show'), 25000)

        await ctx.driver.wait(until.urlContains('/job/show'), 35000)
        let jobShowPage = new JobShowPage(ctx,'SeleniumBasic','')

        await jobShowPage.waitJobDefinition()

        let jobDefinitionModal = await jobShowPage.jobDefinition()
        await jobDefinitionModal.click()

        let pluginAtribute = await jobShowPage.findJobStepDefinition()
        let pluginAtributeText = await pluginAtribute.getText()
        expect(pluginAtributeText).toEqual("${job.id}")

    })



})
