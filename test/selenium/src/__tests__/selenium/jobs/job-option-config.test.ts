import {CreateContext} from '@rundeck/testdeck/test/selenium'
import {ProjectCreatePage} from 'pages/projectCreate.page'
import {LoginPage} from 'pages/login.page'
import {JobCreatePage} from 'pages/jobCreate.page'
import {JobShowPage} from "pages/jobShow.page"
import {Elems as ShowPageElems} from 'pages/jobShow.page'
import {By, until} from 'selenium-webdriver'

import '@rundeck/testdeck/test/rundeck'

// We will initialize and cleanup in the before/after methods
let ctx = CreateContext({projects: ['SeleniumBasic']})
let loginPage: LoginPage
let projectCreate: ProjectCreatePage
let jobCreatePage: JobCreatePage

beforeAll( async () => {
    loginPage = new LoginPage(ctx)
    projectCreate = new ProjectCreatePage(ctx)
    jobCreatePage = new JobCreatePage(ctx, 'SeleniumBasic')
})

beforeAll(async () => {
    await loginPage.login('admin', 'admin')
})


describe('job', () => {
    beforeAll(async () => {
        await jobCreatePage.get()
        ctx.driver.wait(until.alertIsPresent()).then(() => {
            ctx.driver.switchTo().alert().accept();
        });
        await ctx.driver.wait(until.urlContains('/job/create'), 25000)
        await jobCreatePage.waitJobNameInput()
    })
    it('check usage session', async () => {
        let jobNameText='a job with options'
        let jobName=await jobCreatePage.jobNameInput()
        await jobName.sendKeys(jobNameText)

        //add workflow step
        let wfTab=await jobCreatePage.tabWorkflow()
        await wfTab.click()
        let addWfStepCommand=await jobCreatePage.addNewWfStepCommand()

        //click add Command step, and wait until input fields are loaded
        await addWfStepCommand.click()
        await jobCreatePage.waitWfStepCommandRemoteText()


        let wfStepCommandRemoteText=await jobCreatePage.wfStepCommandRemoteText()
        await wfStepCommandRemoteText.sendKeys('echo selenium test')

        let wfStep0SaveButton=await jobCreatePage.wfStep0SaveButton()

        //click step Save button and wait for the step content to display
        await wfStep0SaveButton.click()
        await jobCreatePage.waitWfstep0vis()

        //add options//
        //1. click new option button
        let optionNewButton = await jobCreatePage.optionNewButton()
        await optionNewButton.click()
        //2. wait for edit form to load
        await jobCreatePage.waitoption0EditForm()

        let optionName='seleniumOption1'
        let optionNameInput=await jobCreatePage.option0NameInput()
        await optionNameInput.sendKeys(optionName)

        let optionUsageSection=await jobCreatePage.option0UsageSession()
        expect(optionUsageSection).toBeDefined()

        //save option
        let optionFormSaveButton = await jobCreatePage.optionFormSaveButton()
        await optionFormSaveButton.click()

        //wait for option to save
        await jobCreatePage.waitOption0li()

        //save the job
        let save = await jobCreatePage.saveButton()
        await save.click()

    })

    it('check storage session', async () => {
        let jobNameText='a job with option secure'
        let jobName = await jobCreatePage.jobNameInput()
        await jobName.sendKeys(jobNameText)

        //add workflow step
        let wfTab=await jobCreatePage.tabWorkflow()
        await wfTab.click()
        let addWfStepCommand=await jobCreatePage.addNewWfStepCommand()

        //click add Command step, and wait until input fields are loaded
        await addWfStepCommand.click()
        await jobCreatePage.waitWfStepCommandRemoteText()


        let wfStepCommandRemoteText=await jobCreatePage.wfStepCommandRemoteText()
        await wfStepCommandRemoteText.sendKeys('echo selenium test')

        let wfStep0SaveButton=await jobCreatePage.wfStep0SaveButton()

        //click step Save button and wait for the step content to display
        await wfStep0SaveButton.click()
        await jobCreatePage.waitWfstep0vis()

        //add options//
        //1. click new option button
        let optionNewButton = await jobCreatePage.optionNewButton()
        await optionNewButton.click()
        //2. wait for edit form to load
        await jobCreatePage.waitoption0EditForm()

        let optionName='seleniumOption1'
        let optionNameInput=await jobCreatePage.option0NameInput()
        await optionNameInput.sendKeys(optionName)

        let option0Type=await jobCreatePage.option0Type()
        await option0Type.click()

        let option0KeySelector=await jobCreatePage.option0KeySelector()
        expect(option0KeySelector).toBeDefined()

        let option0OpenKeyStorage=await jobCreatePage.option0OpenKeyStorage()
        await option0OpenKeyStorage.click()

        let storagebrowse=await jobCreatePage.storagebrowse()
        expect(storagebrowse).toBeDefined()

        let storagebrowseClose = await jobCreatePage.storagebrowseClose()
        await storagebrowseClose.click()

    
        //save option
        let optionFormSaveButton = await jobCreatePage.optionFormSaveButton()
        await optionFormSaveButton.click()

        //wait for option to save
        await jobCreatePage.waitOption0li()

        //save the job
        let save = await jobCreatePage.saveButton()
        await save.click()

    })
})
