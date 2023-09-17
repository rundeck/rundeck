import {Context} from '@rundeck/testdeck/context'
import {CreateContext} from '@rundeck/testdeck/test/selenium'
import {ProjectCreatePage} from 'pages/projectCreate.page'
import {LoginPage} from 'pages/login.page'
import {JobCreatePage} from 'pages/jobCreate.page'
import {JobShowPage} from 'pages/jobShow.page'
import {until, By, Key} from 'selenium-webdriver'
import '@rundeck/testdeck/test/rundeck'
import {sleep} from '@rundeck/testdeck/async/util'

// We will initialize and cleanup in the before/after methods
const ctx = CreateContext({projects: ['SeleniumBasic']})
let loginPage: LoginPage
let jobCreatePage: JobCreatePage
let jobShowPage: JobShowPage

beforeAll( async () => {
    loginPage = new LoginPage(ctx)
    jobCreatePage = new JobCreatePage(ctx, 'SeleniumBasic')
    jobShowPage = new JobShowPage(ctx,'SeleniumBasic','')
})

beforeAll(async () => {
    await loginPage.login('admin', 'admin')
})

describe('job', () => {

    it('create job with notifications', async () => {

        await jobCreatePage.get()
        await ctx.driver.wait(until.urlContains('/job/create'), 25000)
        const jobName=await jobCreatePage.jobNameInput()
        await jobName.sendKeys('a job with notifications')

        // add workflow step
        const wfTab=await jobCreatePage.tabWorkflow()
        await wfTab.click()
        const addWfStepCommand=await jobCreatePage.addNewWfStepCommand()

        // click add Command step, and wait until input fields are loaded
        await addWfStepCommand.click()
        await jobCreatePage.waitWfStepCommandRemoteText()


        const wfStepCommandRemoteText=await jobCreatePage.wfStepCommandRemoteText()
        await wfStepCommandRemoteText.sendKeys('echo selenium test')

        const wfStep0SaveButton=await jobCreatePage.wfStep0SaveButton()

        // click step Save button and wait for the step content to display
        const wfstepEditDiv=await ctx.driver.findElement(By.css('#wfli_0 div.wfitemEditForm'))
        await wfStep0SaveButton.click()
        await jobCreatePage.waitWfstep0vis()

        // wait until edit form section for step is removed from dom
        await ctx.driver.wait(until.stalenessOf(wfstepEditDiv), 15000)

        const notificationsTab = await jobCreatePage.notificationsTab()
        await notificationsTab.click()

        const vueNotificationEditSections = await jobCreatePage.vueNotificationEditSections()
        const newEmail = 'test@rundeck.com'
        if (vueNotificationEditSections.length < 1) {
            // legacy ui
            const enableNotificationInput = await jobCreatePage.enableNotificationInput()
            await enableNotificationInput.click()


            const notifyOnsuccessEmail = await jobCreatePage.notifyOnsuccessEmail()
            expect(notifyOnsuccessEmail).toBeDefined()

            await notifyOnsuccessEmail.click()

            const notifySuccessRecipients = await jobCreatePage.notifySuccessRecipients()
            expect(notifySuccessRecipients).toBeDefined()
            await notifySuccessRecipients.clear()
            await notifySuccessRecipients.sendKeys(newEmail)
        } else {
            // vue ui

            const vueAddSuccessbutton = await jobCreatePage.vueAddSuccessbutton()
            await vueAddSuccessbutton.click()

            const vueEditNotificationModal = await jobCreatePage.vueEditNotificationModal()
            const typeDropdown = await jobCreatePage.vueEditNotificationPluginTypeDropdownButton()
            await typeDropdown.click()

            const emailItem = await jobCreatePage.vueEditNotificationPluginTypeDropdownMenuItem('email')
            await sleep(1500)
            await emailItem.click()

            // wait for config section to appear
            const notificationConfig = await jobCreatePage.vueNotificationConfig()

            // fill recipients input section
            const recipientsFormGroup = await jobCreatePage.vueNotificationConfigFillPropText('recipients', newEmail)

            // Optional fill subject input section
            // const subjectFormGroup = await jobCreatePage.vueNotificationConfigFillPropText('subject', 'test subject')

            // find modal save button
            const modalSaveBtn = await jobCreatePage.vueEditNotificationModalSaveBtn()
            await modalSaveBtn.click()
            // wait until modal is hidden
            await jobCreatePage.vueEditNotificationModalHidden()
        }

        // save the job
        const save = await jobCreatePage.saveButton()
        await save.click()

        await ctx.driver.wait(until.urlContains('/job/show'), 35000)


        await jobShowPage.waitJobDefinition()
        const jobDefinitionModal = await jobShowPage.jobDefinition()
        await jobDefinitionModal.click()

        await jobShowPage.waitDefinitionNotificationText()
        const notificationDefinitionText = await jobShowPage.jobDefinitionNotificationText()
        expect(notificationDefinitionText).toEqual('mail to: ' + newEmail)
    })


    it('edit job notifications', async () => {

        await jobCreatePage.get()
        await ctx.driver.wait(until.urlContains('/job/create'), 25000)
        const jobName=await jobCreatePage.jobNameInput()
        await jobName.sendKeys('edit job with notifications')

        // add workflow step
        const wfTab=await jobCreatePage.tabWorkflow()
        await wfTab.click()
        const addWfStepCommand=await jobCreatePage.addNewWfStepCommand()

        // click add Command step, and wait until input fields are loaded
        await addWfStepCommand.click()
        await jobCreatePage.waitWfStepCommandRemoteText()


        const wfStepCommandRemoteText=await jobCreatePage.wfStepCommandRemoteText()
        await wfStepCommandRemoteText.sendKeys('echo selenium test')

        const wfStep0SaveButton=await jobCreatePage.wfStep0SaveButton()

        // click step Save button and wait for the step content to display
        const wfstepEditDiv=await ctx.driver.findElement(By.css('#wfli_0 div.wfitemEditForm'))
        await wfStep0SaveButton.click()
        await jobCreatePage.waitWfstep0vis()

        // wait until edit form section for step is removed from dom
        await ctx.driver.wait(until.stalenessOf(wfstepEditDiv), 15000)

        let save = await jobCreatePage.saveButton()
        await save.click()

        await sleep(1500)

        const jobUUID= await jobShowPage.jobUuidText()

        await jobCreatePage.getEditPage(jobUUID)
        await ctx.driver.wait(until.urlContains('/job/edit'), 30000)

        const notificationsTab = await jobCreatePage.notificationsTab()
        await notificationsTab.click()


        const vueNotificationEditSections = await jobCreatePage.vueNotificationEditSections()
        const newEmail = 'test2@rundeck.com'
        if (vueNotificationEditSections.length < 1) {
            // legacy ui
            const enableNotificationInput = await jobCreatePage.enableNotificationInput()
            await enableNotificationInput.click()


            const notifyOnsuccessEmail = await jobCreatePage.notifyOnsuccessEmail()
            expect(notifyOnsuccessEmail).toBeDefined()

            await notifyOnsuccessEmail.click()

            const notifySuccessRecipients = await jobCreatePage.notifySuccessRecipients()
            expect(notifySuccessRecipients).toBeDefined()
            await notifySuccessRecipients.clear()
            await notifySuccessRecipients.sendKeys(newEmail)
        } else {
            // vue ui
            const vueAddSuccessbutton = await jobCreatePage.vueAddSuccessbutton()
            await vueAddSuccessbutton.click()

            const vueEditNotificationModal = await jobCreatePage.vueEditNotificationModal()
            const typeDropdown = await jobCreatePage.vueEditNotificationPluginTypeDropdownButton()
            await typeDropdown.click()

            const emailItem = await jobCreatePage.vueEditNotificationPluginTypeDropdownMenuItem('email')
            await sleep(1500)
            await emailItem.click()

            // wait for config section to appear
            const notificationConfig = await jobCreatePage.vueNotificationConfig()

            // fill recipients input section
            const recipientsFormGroup = await jobCreatePage.vueNotificationConfigFillPropText('recipients', newEmail)

            // Optional fill subject input section
            // const subjectFormGroup = await jobCreatePage.vueNotificationConfigFillPropText('subject', 'test subject')

            // find modal save button
            const modalSaveBtn = await jobCreatePage.vueEditNotificationModalSaveBtn()
            await modalSaveBtn.click()
            // wait until modal is hidden
            await jobCreatePage.vueEditNotificationModalHidden()
        }
        // save the job
        save = await jobCreatePage.editSaveButton()
        await save.click()

        await ctx.driver.wait(until.urlContains('/job/show'), 15000)

        await jobShowPage.waitJobDefinition()
        const jobDefinitionModal = await jobShowPage.jobDefinition()
        await jobDefinitionModal.click()

        await jobShowPage.waitDefinitionNotificationText()
        const notificationDefinitionText = await jobShowPage.jobDefinitionNotificationText()
        console.log(notificationDefinitionText)
        expect(notificationDefinitionText).toEqual('mail to: ' + newEmail)
    })


    it('context variables job  notifications', async () => {

        await jobCreatePage.get()
        await ctx.driver.wait(until.urlContains('/job/create'), 25000)
        const jobName=await jobCreatePage.jobNameInput()
        await jobName.sendKeys('a job with notifications context variables')

        // add workflow step
        const wfTab=await jobCreatePage.tabWorkflow()
        await wfTab.click()
        const addWfStepCommand=await jobCreatePage.addNewWfStepCommand()

        // click add Command step, and wait until input fields are loaded
        await addWfStepCommand.click()
        await jobCreatePage.waitWfStepCommandRemoteText()

        // add options
        // 1. click new option button
        const optionNewButton = await jobCreatePage.optionNewButton()
        await optionNewButton.click()
        // 2. wait for edit form to load
        await jobCreatePage.waitoption0EditForm()

        const optionName='email'
        const optionNameInput=await jobCreatePage.option0NameInput()
        await optionNameInput.sendKeys(optionName)

        const optionUsageSection=await jobCreatePage.option0UsageSession()
        expect(optionUsageSection).toBeDefined()

        // save option
        const optionFormSaveButton = await jobCreatePage.optionFormSaveButton()
        await optionFormSaveButton.click()

        // wait for option to save
        await jobCreatePage.waitOption0li()


        const wfStepCommandRemoteText=await jobCreatePage.wfStepCommandRemoteText()
        await wfStepCommandRemoteText.sendKeys('echo selenium test')

        const wfStep0SaveButton=await jobCreatePage.wfStep0SaveButton()

        // click step Save button and wait for the step content to display
        const wfstepEditDiv=await ctx.driver.findElement(By.css('#wfli_0 div.wfitemEditForm'))
        await wfStep0SaveButton.click()
        await jobCreatePage.waitWfstep0vis()

        // wait until edit form section for step is removed from dom
        await ctx.driver.wait(until.stalenessOf(wfstepEditDiv), 15000)

        const notificationsTab = await jobCreatePage.notificationsTab()
        await notificationsTab.click()
        const vueAddSuccessbutton = await jobCreatePage.vueAddSuccessbutton()
        await vueAddSuccessbutton.click()

        const vueEditNotificationModal = await jobCreatePage.vueEditNotificationModal()
        const typeDropdown = await jobCreatePage.vueEditNotificationPluginTypeDropdownButton()
        await typeDropdown.click()

        const emailItem = await jobCreatePage.vueEditNotificationPluginTypeDropdownMenuItem('email')
        await sleep(1500)
        await emailItem.click()

        // wait for config section to appear
        const notificationConfig = await jobCreatePage.vueNotificationConfig()

        // fill remoteUrl input section
        await jobCreatePage.vueNotificationConfigFillPropText('recipients','${option')

        await sleep(2000)

        const autocomplteSuggestion = await jobCreatePage.findJobNotificationContextAutocomplete()
        const autocomplteSuggestionText = await autocomplteSuggestion.getText()
        expect(autocomplteSuggestionText).toEqual('${option.email} - Option value')

        await autocomplteSuggestion.click()

        await sleep(2000)

        // find modal save button
        const modalSaveBtn = await jobCreatePage.vueEditNotificationModalSaveBtn()
        await modalSaveBtn.click()
        // wait until modal is hidden
        await jobCreatePage.vueEditNotificationModalHidden()

        // save the job
        const save = await jobCreatePage.saveButton()
        await save.click()

        await ctx.driver.wait(until.urlContains('/job/show'), 35000)

        await jobShowPage.waitJobDefinition()
        const jobDefinitionModal = await jobShowPage.jobDefinition()
        await jobDefinitionModal.click()

        await jobShowPage.waitDefinitionNotificationText()

        const notificationDefinitionText = await jobShowPage.jobDefinitionNotificationText()
        expect(notificationDefinitionText).toEqual('mail to: ${option.email}')

    })
})
