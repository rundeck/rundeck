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
    it('create job with notifications', async () => {
        
        await jobCreatePage.get()
        await ctx.driver.wait(until.urlContains('/job/create'), 25000)
        let jobName=await jobCreatePage.jobNameInput()
        await jobName.sendKeys('a job with notifications')

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
        
        let notificationsTab=await jobCreatePage.notificationsTab()
        await notificationsTab.click()

        let enableNotificationInput= await jobCreatePage.enableNotificationInput()
        await enableNotificationInput.click()

        let notifyOnsuccessEmail= await jobCreatePage.notifyOnsuccessEmail()
        expect(notifyOnsuccessEmail).toBeDefined()

        await notifyOnsuccessEmail.click()

        let notifySuccessRecipients= await jobCreatePage.notifySuccessRecipients()
        expect(notifySuccessRecipients).toBeDefined()
        let newEmail='test@rundeck.com'
        notifySuccessRecipients.clear()
        notifySuccessRecipients.sendKeys(newEmail)

        //save the job
        let save = await jobCreatePage.saveButton()
        await save.click()

        await ctx.driver.wait(until.urlContains('/job/show'), 35000)
        let jobShowPage = new JobShowPage(ctx,'SeleniumBasic','')

        await jobShowPage.waitJobDefinition()
        let jobDefinitionModal = await jobShowPage.jobDefinition()
        await jobDefinitionModal.click()

        await jobShowPage.waitDefinitionNotificationText()
        let notificationDefinitionText = await jobShowPage.jobDefinitionNotificationText()
        expect(notificationDefinitionText).toEqual("mail to: " + newEmail)
    })

    it('edit job notifications', async () => {
        await jobCreatePage.getEditPage('b7b68386-3a52-46dc-a28b-1a4bf6ed87de')
        await ctx.driver.wait(until.urlContains('/job/edit'), 30000)

        let notificationsTab=await jobCreatePage.notificationsTab()
        await notificationsTab.click()


        let enableNotificationInput= await jobCreatePage.enableNotificationInput()
        await enableNotificationInput.click()

        let notifyOnsuccessEmail= await jobCreatePage.notifyOnsuccessEmail()
        expect(notifyOnsuccessEmail).toBeDefined()

        await notifyOnsuccessEmail.click()

        let notifySuccessRecipients= await jobCreatePage.notifySuccessRecipients()
        expect(notifySuccessRecipients).toBeDefined()
        let newEmail='test@rundeck.com'
        notifySuccessRecipients.clear()
        notifySuccessRecipients.sendKeys(newEmail)

        //save the job
        let save = await jobCreatePage.editSaveButton()
        await save.click()

        await ctx.driver.wait(until.urlContains('/job/show'), 35000)
        let jobShowPage = new JobShowPage(ctx,'SeleniumBasic','')

        await jobShowPage.waitJobDefinition()

        let jobDefinitionModal = await jobShowPage.jobDefinition()
        await jobDefinitionModal.click()

        await jobShowPage.waitDefinitionNotificationText()
        let notificationDefinitionText = await jobShowPage.jobDefinitionNotificationText()
        expect(notificationDefinitionText).toEqual("mail to: " + newEmail)
    })
})