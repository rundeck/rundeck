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

beforeAll(async () => {
    await loginPage.login('admin', 'admin')
})

describe('edit job', () => {
    beforeEach(async () => {
        await jobCreatePage.getEditPage('b7b68386-3a52-46dc-a28b-1a4bf6ed87de')
        await ctx.driver.wait(until.urlContains('/job/edit'), 30000)
    })
    it('adds and saves tags correctly', async () => {
        const tagsField = await jobCreatePage.tagsInputArea()
        await ctx.driver.wait(until.elementIsVisible(tagsField))
        expect(tagsField).toBeDefined()
        const tags = 'test1,test2,test3,'
        tagsField.sendKeys(tags)

        //ensure they were tag-ified
        const pilledTagsField = await jobCreatePage.tagsPillsArea()
        expect(pilledTagsField).toBeDefined()

        const save = await jobCreatePage.editSaveButton()
        await save.click()

        await ctx.driver.wait(until.urlContains('/job/show'), 21000)

        //verify tags post save
        const showTags = await jobShowPage.jobTags()
        expect(showTags).toHaveLength(3)
    })
    it('edit job description', async () => {
        let descriptionTextField = await jobCreatePage.descriptionTextarea()
        expect(descriptionTextField).toBeDefined()

        //NB: in order to edit the description, we seem to have to bypass the Ace JS text editor
        //make textarea visible
        await ctx.driver.executeScript('jQuery(\'form textarea[name="description"]\').show()')
        await ctx.driver.wait(until.elementIsVisible(descriptionTextField))
        const newDescriptionText = 'a new job description'
        descriptionTextField.clear()
        descriptionTextField.sendKeys(newDescriptionText)

        //save the job
        const save = await jobCreatePage.editSaveButton()
        await save.click()

        await ctx.driver.wait(until.urlContains('/job/show'), 15000)

        //verfiy job description
        const foundText = await jobShowPage.jobDescriptionText()
        expect(foundText).toEqual(newDescriptionText)

        //verify options exist
        await ctx.driver.wait(until.elementLocated(By.css('#optionSelect')), 10000)
        //get option input field
        let optionRunInput1 = await jobShowPage.optionInputText('seleniumOption1')
        expect(optionRunInput1).toBeDefined()

        let optionRunInput2 = await jobShowPage.optionInputText('xyz')
        expect(optionRunInput2).toBeDefined()

    })
})