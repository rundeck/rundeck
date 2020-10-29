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
    it('create job with dispatch to nodes', async () => {
        
        await jobCreatePage.get()
        await ctx.driver.wait(until.urlContains('/job/create'), 25000)
        let jobName=await jobCreatePage.jobNameInput()
        await jobName.sendKeys('jobs with nodes')

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
        let wfstepEditDiv=await ctx.driver.findElement(By.css('#wfli_0 div.wfitemEditForm'))
        await wfStep0SaveButton.click()
        await jobCreatePage.waitWfstep0vis()

        //wait until edit form section for step is removed from dom
        await ctx.driver.wait(until.stalenessOf(wfstepEditDiv), 15000)
        
        let tabNodes=await jobCreatePage.tabNodes()
        await tabNodes.click()

        let enableDispatchNodes= await jobCreatePage.dispatchNodes()
        await enableDispatchNodes.click()

        let nodeFilter= await jobCreatePage.nodeFilter()
        expect(nodeFilter).toBeDefined()

        let nodeFilterButton= await jobCreatePage.nodeFilterButton()
        expect(nodeFilterButton).toBeDefined()

        await nodeFilterButton.click()

        let nodeFilterSelectAllLink= await jobCreatePage.nodeFilterSelectAllLink()
        await nodeFilterSelectAllLink.click()
        
        let matchedNodesText= await jobCreatePage.matchedNodesText()
        expect(matchedNodesText).toEqual("1 Node Matched")

        //save the job
        let save = await jobCreatePage.saveButton()
        await save.click()

    })

    
})