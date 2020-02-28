import {Context} from 'context'
import {CreateContext} from 'test/selenium'
import {ProjectCreatePage} from 'pages/projectCreate.page'
import {LoginPage} from 'pages/login.page'
import {JobCreatePage} from 'pages/jobCreate.page'
import {JobShowPage} from "pages/jobShow.page"
import {until, By, Key} from 'selenium-webdriver'
import {sleep} from 'async/util';
import 'test/rundeck'

// We will initialize and cleanup in the before/after methods
let ctx: Context
let loginPage: LoginPage
let jobCreatePage: JobCreatePage

beforeAll( async () => {
    ctx = await CreateContext()
    loginPage = new LoginPage(ctx)
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


describe('job', () => {
    it('edit job description', async () => {
        await jobCreatePage.getEditPage('b7b68386-3a52-46dc-a28b-1a4bf6ed87de')
        await ctx.driver.wait(until.urlContains('/job/edit'), 30000)
        let descriptionTextField= await jobCreatePage.descriptionTextarea()
        expect(descriptionTextField).toBeDefined()

        //NB: in order to edit the description, we seem to have to bypass the Ace JS text editor
        //make textarea visible
        await ctx.driver.executeScript('jQuery(\'form textarea[name="description"]\').show()')
        await ctx.driver.wait(until.elementIsVisible(descriptionTextField))
        let newDescriptionText='a new job description'
        descriptionTextField.clear()
        descriptionTextField.sendKeys(newDescriptionText)

        //save the job
        let save = await jobCreatePage.editSaveButton()
        await save.click()

        await ctx.driver.wait(until.urlContains('/job/show'), 15000)
        let jobShowPage = new JobShowPage(ctx,'SeleniumBasic','')

        //verfiy job description
        let foundText=await jobShowPage.jobDescriptionText()
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