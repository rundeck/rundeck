import {Context} from '@rundeck/testdeck/context'
import {CreateContext} from '@rundeck/testdeck/test/selenium'
import {ProjectCreatePage} from 'pages/projectCreate.page'
import {LoginPage} from 'pages/login.page'
import {JobCreatePage} from 'pages/jobCreate.page'
import {JobShowPage} from "pages/jobShow.page"
import {until, By, Key} from 'selenium-webdriver'
import '@rundeck/testdeck/test/rundeck'

// We will initialize and cleanup in the before/after methods
let ctx = CreateContext({projects: ['SeleniumBasic']})
let loginPage: LoginPage
let jobCreatePage: JobCreatePage

beforeAll( async () => {
    loginPage = new LoginPage(ctx)
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
    })

    it('job workflow simple undo', async () => {
        let jobNameText='a job with workflow undo test'
        let jobName=await jobCreatePage.jobNameInput()
        await jobName.sendKeys(jobNameText)

        //add workflow step
        let wfTab=await jobCreatePage.tabWorkflow()
        await wfTab.click()
        let addWfStepCommand = await jobCreatePage.addNewWfStepCommand()
        await ctx.driver.wait(until.elementIsEnabled(addWfStepCommand), 15000)

        //click add Command step, and wait until input fields are loaded
        await addWfStepCommand.click()
        await jobCreatePage.waitWfStepCommandRemoteText()


        let wfStepCommandRemoteText=await jobCreatePage.wfStepCommandRemoteText()
        await wfStepCommandRemoteText.sendKeys('echo selenium test')

        let wfStep0SaveButton=await jobCreatePage.wfStepSaveButton('0')

        //click step Save button and wait for the step content to display
        await wfStep0SaveButton.click()
        await jobCreatePage.waitWfstepvis('0')

        // add step 2
        await jobCreatePage.waitAddNewWfStepButton()

        let addNewWfStepButton = await jobCreatePage.addNewWfStepButton()
        expect(addNewWfStepButton).toBeDefined()

        await addNewWfStepButton.click()

        addWfStepCommand=await jobCreatePage.addNewWfStepCommand()

        //click add Command step, and wait until input fields are loaded
        await addWfStepCommand.click()
        await jobCreatePage.waitWfStepCommandRemoteText()


        wfStepCommandRemoteText = await jobCreatePage.wfStepCommandRemoteText()
        await wfStepCommandRemoteText.sendKeys('echo selenium test 2')

        let wfStep1SaveButton = await jobCreatePage.wfStepSaveButton('1')

        //click step Save button and wait for the step content to display
        await wfStep1SaveButton.click()
        await jobCreatePage.waitWfstepvis('1')

        //wait for undo button
        await jobCreatePage.waitUndoRedo(5000)
        await jobCreatePage.waitWfUndoButton()

        let wfUndo = await jobCreatePage.wfUndoButton()
        expect(wfUndo).toBeDefined()

        await wfUndo.click()
        await jobCreatePage.waitUndoRedo(5000)

        let isWfli1 = await jobCreatePage.isWfli("1")
        expect(isWfli1).toEqual(false)

        //save the job
        let save = await jobCreatePage.saveButton()
        await save.click()

        await ctx.driver.wait(until.urlContains('/job/show'), 25000)

    })

    it('job workflow undo redo', async () => {
        let jobNameText='a job with workflow undo-redo test'
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

        let wfStep0SaveButton=await jobCreatePage.wfStepSaveButton('0')

        //click step Save button and wait for the step content to display
        await wfStep0SaveButton.click()
        await jobCreatePage.waitWfstepvis('0')

        // add step 2
        await jobCreatePage.waitAddNewWfStepButton()

        let addNewWfStepButton = await jobCreatePage.addNewWfStepButton()
        expect(addNewWfStepButton).toBeDefined()

        await addNewWfStepButton.click()

        addWfStepCommand=await jobCreatePage.addNewWfStepCommand()

        //click add Command step, and wait until input fields are loaded
        await addWfStepCommand.click()
        await jobCreatePage.waitWfStepCommandRemoteText()


        wfStepCommandRemoteText = await jobCreatePage.wfStepCommandRemoteText()
        await wfStepCommandRemoteText.sendKeys('echo selenium test 2')

        let wfStep1SaveButton = await jobCreatePage.wfStepSaveButton('1')

        //click step Save button and wait for the step content to display
        await wfStep1SaveButton.click()
        await jobCreatePage.waitWfstepvis('1')

        // wait for undo button
        await jobCreatePage.waitUndoRedo(5000)


        await jobCreatePage.waitWfUndoButton()

        let wfUndo = await jobCreatePage.wfUndoButton()
        expect(wfUndo).toBeDefined()

        await wfUndo.click()
        await jobCreatePage.waitUndoRedo(5000)

        await jobCreatePage.waitWfRedoButton()
        let wfRedo = await jobCreatePage.wfRedoButton()
        expect(wfRedo).toBeDefined()

        await wfRedo.click()
        await jobCreatePage.waitUndoRedo(5000)

        let isWfli1=await jobCreatePage.isWfli("1")
        expect(isWfli1).toEqual(true)

        //save the job
        let save = await jobCreatePage.saveButton()
        await save.click()

        await ctx.driver.wait(until.urlContains('/job/show'), 25000)

    })

    it('job workflow revert all', async () => {
        let jobNameText='a job with workflow revert all test'
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

        let wfStep0SaveButton=await jobCreatePage.wfStepSaveButton('0')

        //click step Save button and wait for the step content to display
        await wfStep0SaveButton.click()
        await jobCreatePage.waitWfstepvis('0')

        // add step 2
        await jobCreatePage.waitAddNewWfStepButton()

        let addNewWfStepButton = await jobCreatePage.addNewWfStepButton()
        expect(addNewWfStepButton).toBeDefined()

        await jobCreatePage.waitUndoRedo(5000)
        await addNewWfStepButton.click()

        addWfStepCommand=await jobCreatePage.addNewWfStepCommand()

        //click add Command step, and wait until input fields are loaded
        await addWfStepCommand.click()
        await jobCreatePage.waitWfStepCommandRemoteText()

        wfStepCommandRemoteText = await jobCreatePage.wfStepCommandRemoteText()
        await wfStepCommandRemoteText.sendKeys('echo selenium test 2')

        let wfStep1SaveButton = await jobCreatePage.wfStepSaveButton('1')

        //click step Save button and wait for the step content to display
        await wfStep1SaveButton.click()
        await jobCreatePage.waitWfstepvis('1')

        //wait for revert button
        await jobCreatePage.waitUndoRedo(5000)

        await jobCreatePage.waitRevertWfButton()

        let revertWfButton = await jobCreatePage.revertWfButton()
        expect(revertWfButton).toBeDefined()

        await revertWfButton.click()
        await jobCreatePage.waitUndoRedo(5000)

        let revertWfConfirm = await jobCreatePage.revertWfConfirm()
        expect(revertWfConfirm).toBeDefined()

        await revertWfConfirm.click()
        await jobCreatePage.waitUndoRedo(5000)

        let isWfli0=await jobCreatePage.isWfli("0")
        expect(isWfli0).toEqual(false)
        let isWfli1=await jobCreatePage.isWfli("1")
        expect(isWfli1).toEqual(false)

        //add final step
        // add step 2
        await jobCreatePage.waitAddNewWfStepButton()

        addNewWfStepButton = await jobCreatePage.addNewWfStepButton()
        expect(addNewWfStepButton).toBeDefined()

        await addNewWfStepButton.click()

        addWfStepCommand=await jobCreatePage.addNewWfStepCommand()

        //click add Command step, and wait until input fields are loaded
        await addWfStepCommand.click()
        await jobCreatePage.waitWfStepCommandRemoteText()


        wfStepCommandRemoteText=await jobCreatePage.wfStepCommandRemoteText()
        await wfStepCommandRemoteText.sendKeys('echo selenium test final')

        wfStep0SaveButton=await jobCreatePage.wfStepSaveButton('0')

        //click step Save button and wait for the step content to display
        await wfStep0SaveButton.click()
        await jobCreatePage.waitWfstepvis('0')

        //save the job
        let save = await jobCreatePage.saveButton()
        await save.click()

        await ctx.driver.wait(until.urlContains('/job/show'), 25000)

    })


})
