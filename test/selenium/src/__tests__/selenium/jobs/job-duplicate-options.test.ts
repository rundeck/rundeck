import {CreateContext} from '@rundeck/testdeck/test/selenium'
import {JobCreatePage} from 'pages/jobCreate.page'
import {JobShowPage} from 'pages/jobShow.page'
import {Elems as ShowPageElems} from 'pages/jobShow.page'
import {LoginPage} from 'pages/login.page'
import {ProjectCreatePage} from 'pages/projectCreate.page'
import {By, until} from 'selenium-webdriver'

import '@rundeck/testdeck/test/rundeck'

// We will initialize and cleanup in the before/after methods
const ctx = CreateContext({projects: ['SeleniumBasic']})
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

    it('duplicate_options', async () => {
        await jobCreatePage.get()
        await ctx.driver.wait(until.urlContains('/job/create'), 25000)
        const jobNameText = 'duplicate options'
        const jobName = await jobCreatePage.jobNameInput()
        await jobName.sendKeys(jobNameText)

        // add workflow step
        const wfTab = await jobCreatePage.tabWorkflow()
        await wfTab.click()
        const addWfStepCommand = await jobCreatePage.addNewWfStepCommand()

        // click add Command step, and wait until input fields are loaded
        await addWfStepCommand.click()
        await jobCreatePage.waitWfStepCommandRemoteText()

        const wfStepCommandRemoteText = await jobCreatePage.wfStepCommandRemoteText()
        await wfStepCommandRemoteText.sendKeys('echo selenium test')

        const wfStep0SaveButton = await jobCreatePage.wfStep0SaveButton()

        // click step Save button and wait for the step content to display
        await wfStep0SaveButton.click()
        await jobCreatePage.waitWfstep0vis()

        // add options//
        // 1. click new option button
        const optionNewButton = await jobCreatePage.optionNewButton()
        await optionNewButton.click()
        // 2. wait for edit form to load
        await jobCreatePage.waitoption0EditForm()

        const optionName = 'test'
        const optionNameInput = await jobCreatePage.optionNameInput('0')
        await optionNameInput.sendKeys(optionName)

        const optionUsageSection = await jobCreatePage.option0UsageSession()
        expect(optionUsageSection).toBeDefined()

        // save option
        const optionFormSaveButton = await jobCreatePage.optionFormSaveButton()
        await optionFormSaveButton.click()

        // wait for option to save
        await jobCreatePage.waitOptionli('0')

        // duplicate option 0

        let optionDuplicateButton = await jobCreatePage.optionDuplicateButton(optionName)
        await optionDuplicateButton.click()

        await jobCreatePage.waitOptionli('1')

        let duplicatedOption = await jobCreatePage.isOptionli('1')
        expect(duplicatedOption).toEqual(true)

        let duplicateOptionName = await jobCreatePage.optionNameText('1')
        expect(duplicateOptionName).toEqual(optionName + '_1')

        // duplicate option 0 again
        optionDuplicateButton = await jobCreatePage.optionDuplicateButton(optionName)
        await optionDuplicateButton.click()

        await jobCreatePage.waitOptionli('2')

        duplicatedOption = await jobCreatePage.isOptionli('2')
        expect(duplicatedOption).toEqual(true)

        duplicateOptionName = await jobCreatePage.optionNameText('2')
        expect(duplicateOptionName).toEqual(optionName + '_2')

        // save the job
        const saveButton = await jobCreatePage.saveButton()
        await ctx.driver.executeScript('arguments[0].scrollIntoView(true);', saveButton)
        await saveButton.click()

    })
})
