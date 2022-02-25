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
let jobShowPage: JobShowPage

beforeAll( async () => {
    loginPage = new LoginPage(ctx)
    jobCreatePage = new JobCreatePage(ctx, 'SeleniumBasic')
    jobShowPage = new JobShowPage(ctx, 'SeleniumBasic', '')
})

const testVars = {
    group: 'testGroup',
    newDescriptionText: 'a new job description',
    optionInputText: ['seleniumOption1', 'xyz'],
    tags: 'test1,test2,test3,',
    tagsCount: 3,
    scheduledDay: {name: 'every day', index: 0},
}

beforeAll(async () => {
    await loginPage.login('admin', 'admin')
})

describe('editing a job', () => {
    beforeAll(async () => {
        await jobCreatePage.getEditPage('b7b68386-3a52-46dc-a28b-1a4bf6ed87de')
        await ctx.driver.wait(until.urlContains('/job/edit'), 30000)
        await ctx.driver.wait(until.elementLocated(By.css('div#schedJobNameLabel')), 10000)
    })
    afterAll(async () => {
        const save = await jobCreatePage.editSaveButton()
        await save.click()
    })
    describe('details tab', () => {
        it('edits and saves job description correctly', async () => {
            const descriptionTextField = await jobCreatePage.descriptionTextarea()
            expect(descriptionTextField).toBeDefined()

            // NB: in order to edit the description, we seem to have to bypass the Ace JS text editor
            // make textarea visible
            await ctx.driver.executeScript('jQuery(\'form textarea[name="description"]\').show()')
            await ctx.driver.wait(until.elementIsVisible(descriptionTextField))
            descriptionTextField.clear()
            descriptionTextField.sendKeys(testVars.newDescriptionText)
        })
        it('correctly sets the group', async () => {
            const chooseGroupInput = await jobCreatePage.groupChooseInput()
            await ctx.driver.wait(until.elementIsVisible(chooseGroupInput))
            expect(chooseGroupInput).toBeDefined()
            chooseGroupInput.sendKeys(testVars.group)
        })
    })
    describe('schedules tab', () => {
        beforeAll(async () => {
            const scheduleTab = await jobCreatePage.scheduleTab()
            const scheduleRunYes = await jobCreatePage.scheduleRunYes()
            await scheduleTab.click()
            await ctx.driver.wait(until.urlContains('#schedule'), 15000)
            await scheduleRunYes.click()
        })
        it(`sets the job to run on ${testVars.scheduledDay.name}`, async () => {
            const everyDayOfWeekCheckbox = await jobCreatePage.scheduleEveryDayCheckbox();
            const scheduleDaysCheckboxDiv = await jobCreatePage.scheduleDaysCheckboxDiv();
            if (!await everyDayOfWeekCheckbox.isSelected()) {
                await everyDayOfWeekCheckbox.click()
            }

            expect(await scheduleDaysCheckboxDiv.isDisplayed()).toEqual(true)
        })
    })
})

describe('showing the edited job', () => {
    beforeAll(async () => {
        await ctx.driver.wait(until.urlContains('/job/show'), 15000)
    })
    it('verifies job group', async () => {
        const groupLabel = await jobShowPage.jobGroupText()
        expect(groupLabel).toEqual(testVars.group)
    })
    it('verifies job description', async () => {
        const foundText = await jobShowPage.jobDescriptionText()
        expect(foundText).toEqual(testVars.newDescriptionText)
    })
    it('verifies options exist', async () => {
        await ctx.driver.wait(until.elementLocated(By.css('#optionSelect')), 10000)

        const optionRunInput1 = await jobShowPage.optionInputText(testVars.optionInputText[0])
        expect(optionRunInput1).toBeDefined()

        const optionRunInput2 = await jobShowPage.optionInputText(testVars.optionInputText[1])
        expect(optionRunInput2).toBeDefined()
    })
    it(`verifies scheduled day (${testVars.scheduledDay.name})`, async () => {
        const jobDefButton = await jobShowPage.jobDefinition();
        const jobDefModal = await jobShowPage.jobDefinitionModal();
        const everyDaySelected = await jobShowPage.jobDefModalScheduleEveryDaySelected();

        await jobDefButton.click();
        await ctx.driver.wait(until.elementIsVisible(jobDefModal));

        expect(await everyDaySelected.getText()).toEqual(testVars.scheduledDay.name);
    })
})