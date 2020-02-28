import {Context} from 'context'
import {CreateContext} from 'test/selenium'

import {JobsListPage,Elems} from "../../pages/jobsList.page"
import {LoginPage} from 'pages/login.page'

import {By, until} from 'selenium-webdriver'
import 'test/rundeck'

// We will initialize and cleanup in the before/after methods
let ctx: Context
let loginPage: LoginPage
let jobsListPage: JobsListPage

beforeAll(async () => {
  ctx = await CreateContext()
  loginPage = new LoginPage(ctx)
  jobsListPage = new JobsListPage(ctx, 'SeleniumBasic')
  await loginPage.login('admin', 'admin')
})

afterAll(async () => {
  if (ctx)
    await ctx.dispose()
})

beforeEach(async () => {
  ctx.currentTestName = expect.getState().currentTestName
})

afterEach(async () => {
  await ctx.screenSnap('final')
})

describe('job list', () => {

  it('run job modal should show validation error', async () => {
    await jobsListPage.get()
    await ctx.driver.wait(until.urlContains('/jobs'), 5000)

    let runJobLink = await jobsListPage.getRunJobLink('0088e04a-0db3-4b03-adda-02e8a4baf709')
    await runJobLink.click()
    //wait for modal to become available
    await ctx.driver.wait(until.elementLocated(Elems.modalOptions),10000)

    //locate run job button

    let runJobNowButton = await jobsListPage.getRunJobNowLink()
    await runJobNowButton.click()
    //wait for modal to become available agaih
    await ctx.driver.wait(until.elementLocated(Elems.modalOptions),10000)

    //expect option is required message
    await ctx.driver.wait(until.elementLocated(Elems.optionValidationWarningText),10000)

    //expect warning text to say option is required

    let optionWarningText = await jobsListPage.getOptionWarningText()

    expect(optionWarningText).toContain('Option \'reqOpt1\' is required')
  })
})
