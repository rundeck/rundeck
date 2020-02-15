import {Context} from 'context'
import {CreateContext} from 'test/selenium'
import {ProjectCreatePage} from 'pages/projectCreate.page'
import {LoginPage} from 'pages/login.page'
import {JobCreatePage} from 'pages/jobCreate.page'
import {JobShowPage} from "../../pages/jobShow.page"
import {Elems as ShowPageElems} from '../../pages/jobShow.page'
import {until} from 'selenium-webdriver'
import {sleep} from 'async/util';
import 'test/rundeck'

// We will initialize and cleanup in the before/after methods
let ctx: Context
let loginPage: LoginPage
let projectCreate: ProjectCreatePage
let jobCreatePage: JobCreatePage

beforeAll( async () => {
    ctx = await CreateContext()
    loginPage = new LoginPage(ctx)
    projectCreate = new ProjectCreatePage(ctx)
    jobCreatePage = new JobCreatePage(ctx, 'SeleniumBasic')
})

beforeEach( async () => {
    ctx.currentTestName = expect.getState().currentTestName
})

afterAll( async () => {
    if (ctx)
        await ctx.dispose()
})

// afterEach( async () => {
//     await ctx.screenSnap('final')
// })

beforeAll(async () => {
    await loginPage.login('admin', 'admin')
})

describe('project', () => {
    // it('has not visually regressed', async () => {
    //     await projectCreate.get()
    //     const img = Buffer.from(await projectCreate.screenshot(true), 'base64')
    //     expect(img).toMatchImageSnapshot({customSnapshotsDir: '__image_snapshots__', customDiffConfig: {threshold: 0.01}})
    // })
    it('has basic fields', async () => {
        await projectCreate.get()
        await ctx.driver.wait(until.urlContains('/resources/createProject'), 5000)
        expect(projectCreate.projectNameInput()).resolves.toBeDefined()
        expect(projectCreate.labelInput()).resolves.toBeDefined()
        expect(projectCreate.descriptionInput()).resolves.toBeDefined()
    })
})

describe('job', () => {
    it('has basic fields', async () => {
        await jobCreatePage.get()
        await ctx.driver.wait(until.urlContains('/job/create'), 15000)
        expect(jobCreatePage.jobNameInput()).resolves.toBeDefined()
        expect(jobCreatePage.groupPathInput()).resolves.toBeDefined()
        expect(jobCreatePage.descriptionTextarea()).resolves.toBeDefined()
        
    })
    it('invalid empty name', async () => {
        await jobCreatePage.get()
        await ctx.driver.wait(until.urlContains('/job/create'), 15000)
        let jobName=await jobCreatePage.jobNameInput()
        await jobName.clear()
        let save = await jobCreatePage.saveButton()
        await save.click()
        
        await ctx.driver.wait(until.titleMatches(/.*(Create New Job).*$/i), 5000)
        //verify error messages
        let error = await jobCreatePage.errorAlert()
        expect(error.getText()).resolves.toContain('Error saving Job')

        //verify validation message
        let validation = await jobCreatePage.formValidationAlert()
        let text = await validation.getText()
        expect(text).toContain('"Job Name" parameter cannot be blank')
        expect(text).toContain('Workflow must have at least one step')
    })

    it('invalid empty workflow', async () => {
        await jobCreatePage.get()
        await ctx.driver.wait(until.urlContains('/job/create'), 15000)
        let jobName=await jobCreatePage.jobNameInput()
        await jobName.sendKeys('a job')
        let save = await jobCreatePage.saveButton()
        await save.click()
        
        await ctx.driver.wait(until.titleMatches(/.*(Create New Job).*$/i), 5000)
        //verify error messages
        let error = await jobCreatePage.errorAlert()
        expect(error.getText()).resolves.toContain('Error saving Job')

        //verify validation message
        let validation = await jobCreatePage.formValidationAlert()
        let text = await validation.getText()
        expect(text).not.toContain('"Job Name" parameter cannot be blank')
        expect(text).toContain('Workflow must have at least one step')
    })

    it('create valid job basic workflow', async () => {
        await jobCreatePage.get()
        await ctx.driver.wait(until.urlContains('/job/create'), 15000)
        let jobName=await jobCreatePage.jobNameInput()
        await jobName.sendKeys('a job')

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

        let save = await jobCreatePage.saveButton()
        await save.click()

        await ctx.driver.wait(until.urlContains('/job/show'), 5000)
        //verify job name
        let jobTitleLink = await ctx.driver.findElement(ShowPageElems.jobTitleLink)
        let jobTitleText = await jobTitleLink.getText()
        expect(jobTitleText).toContain('a job')

    })

    it('create valid job basic options', async () => {
        await jobCreatePage.get()
        await ctx.driver.wait(until.urlContains('/job/create'), 15000)
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

        //save option
        let optionFormSaveButton = await jobCreatePage.optionFormSaveButton()
        await optionFormSaveButton.click()

        //wait for option to save
        await jobCreatePage.waitOption0li()

        //save the job
        let save = await jobCreatePage.saveButton()
        await save.click()

        await ctx.driver.wait(until.urlContains('/job/show'), 5000)
        let jobShowPage = new JobShowPage(ctx,'SeleniumBasic','')
        
        //verify job name

        let jobTitleText = await jobShowPage.jobTitleText()        
        expect(jobTitleText).toContain(jobNameText)

        //get option input field
        let optionRunInput = jobShowPage.optionInputText(optionName)
        expect(optionRunInput).not.toBeUndefined()


    })
    // it('has not visually regressed', async () => {
    //     await jobCreatePage.get()
    //     const img = Buffer.from(await jobCreatePage.screenshot(true), 'base64')
    //     expect(img).toMatchImageSnapshot({customSnapshotsDir: '__image_snapshots__', customDiffConfig: {threshold: 0.01}})
    // })
})
