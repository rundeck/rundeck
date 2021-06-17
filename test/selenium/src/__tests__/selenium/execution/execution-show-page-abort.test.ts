import {sleep} from '@rundeck/testdeck/async/util'
import {Context} from '@rundeck/testdeck/context'
import '@rundeck/testdeck/test/rundeck'
import {CreateContext} from '@rundeck/testdeck/test/selenium'
import {AdhocCommandPage, Elems} from 'pages/adhocCommand.page'
import {ExecutionShowPage} from 'pages/executionShow.page'
import {LoginPage} from 'pages/login.page'
import {ProjectCreatePage} from 'pages/projectCreate.page'
import {By, Key, until} from 'selenium-webdriver'

// We will initialize and cleanup in the before/after methods
const ctx = CreateContext({projects: ['SeleniumBasic']})
let loginPage: LoginPage
let adhocPage: AdhocCommandPage

beforeAll(async () => {
  loginPage = new LoginPage(ctx)
  adhocPage = new AdhocCommandPage(ctx, 'SeleniumBasic')
})

beforeAll(async () => {
  await loginPage.login('admin', 'admin')
})

describe('execution', () => {
  it('abort button in show page', async () => {

    await adhocPage.get()
    await ctx.driver.wait(until.urlContains('/command/run'), 25000)
    const nodeFilter = await adhocPage.nodeFilterInput()
    await nodeFilter.sendKeys('.*')

    const nodeFilterBtn = await adhocPage.filterNodesButton()
    await nodeFilterBtn.click()

    const commandInput = await adhocPage.commandInput()
    await commandInput.sendKeys('echo running test && sleep 45')

    const commandButton = await adhocPage.runCommandButton()
    await commandButton.click()

    // find execution id and visit execution page

    await ctx.driver.wait(until.elementLocated(Elems.runningExecutionState), 25000)

    const execPageLink = await adhocPage.runningExecutionLink()
    await execPageLink.click()

    await ctx.driver.wait(until.urlContains('/execution/show'), 25000)
    const execShowPage = new ExecutionShowPage(ctx, 'SeleniumBasic', '')

    // view execution as running
    const display = await execShowPage.executionStateDisplay()
    const val = await display.getAttribute('data-execstate')
    expect(val.toUpperCase()).toEqual('RUNNING')

    // invoke abort button
    const abortButton = await execShowPage.abortButton()
    await abortButton.click()

    // sleep
    await sleep(3000)

    // expect state to change to Aborted
    const val2 = await display.getAttribute('data-execstate')
    expect(val2.toUpperCase()).toEqual('ABORTED')

  })

})
