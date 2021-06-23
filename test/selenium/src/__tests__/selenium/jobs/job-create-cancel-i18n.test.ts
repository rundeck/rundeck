import {CreateContext} from '@rundeck/testdeck/test/selenium'
import {JobCreatePage} from 'pages/jobCreate.page'
import {LoginPage} from 'pages/login.page'
import {ProjectCreatePage} from 'pages/projectCreate.page'
import {UserProfilePage} from 'pages/userProfile.page'
import {By, until} from 'selenium-webdriver'

import '@rundeck/testdeck/test/rundeck'

// We will initialize and cleanup in the before/after methods
const ctx = CreateContext({projects: ['SeleniumBasic']})
let loginPage: LoginPage
let projectCreate: ProjectCreatePage
let jobCreatePage: JobCreatePage

beforeAll(async () => {
  loginPage = new LoginPage(ctx)
  projectCreate = new ProjectCreatePage(ctx)
  jobCreatePage = new JobCreatePage(ctx, 'SeleniumBasic')
})

beforeAll(async () => {
  await loginPage.login('admin', 'admin')
})

describe('job', () => {

  it('cancel job create with default lang', async () => {
    await jobCreatePage.get()
    await ctx.driver.wait(until.urlContains('/job/create'), 25000)

    // cancel save
    const cancel = await jobCreatePage.cancelButton()
    await cancel.click()

    await ctx.driver.wait(until.urlContains('/jobs'), 15000)

  })
  it('change UI lang fr_FR and cancel job create', async () => {
    const userProfileLangFR = new UserProfilePage(ctx, 'fr_FR')
    await userProfileLangFR.get()
    // assert french is used
    const label = await userProfileLangFR.languageLabel()
    const labelText = await label.getText()
    expect(labelText).toBe('Langue:')

    await jobCreatePage.get()
    await ctx.driver.wait(until.urlContains('/job/create'), 25000)

    // cancel save
    const cancel = await jobCreatePage.cancelButton()
    await cancel.click()

    await ctx.driver.wait(until.urlContains('/jobs'), 15000)

  })
  it('change UI lang ja_JP and cancel job create', async () => {
    const userProfileLangJA = new UserProfilePage(ctx, 'ja_JP')
    await userProfileLangJA.get()
    // assert french is used
    const label = await userProfileLangJA.languageLabel()
    const labelText = await label.getText()
    expect(labelText).toBe('言語:')

    await jobCreatePage.get()
    await ctx.driver.wait(until.urlContains('/job/create'), 25000)

    // cancel save
    const cancel = await jobCreatePage.cancelButton()
    await cancel.click()

    await ctx.driver.wait(until.urlContains('/jobs'), 15000)

  })

})
