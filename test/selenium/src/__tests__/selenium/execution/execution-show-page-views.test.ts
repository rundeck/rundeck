import {sleep} from '@rundeck/testdeck/async/util'
import {Context} from '@rundeck/testdeck/context'
import '@rundeck/testdeck/test/rundeck'
import {CreateContext} from '@rundeck/testdeck/test/selenium'
import {AdhocCommandPage, Elems} from 'pages/adhocCommand.page'
import {ExecutionShowPage} from 'pages/executionShow.page'
import {LoginPage} from 'pages/login.page'
import {ProjectCreatePage} from 'pages/projectCreate.page'
import {By, Key, until} from 'selenium-webdriver'
import {NavigationPage} from '../../../pages/navigation.page'

// We will initialize and cleanup in the before/after methods
const ctx = CreateContext({projects: ['SeleniumBasic']})
let loginPage: LoginPage
let adhocPage: AdhocCommandPage
let execPageHref: string
let navigation: NavigationPage

beforeAll(async () => {
  loginPage = new LoginPage(ctx)
  navigation = new NavigationPage(ctx)
  adhocPage = new AdhocCommandPage(ctx, 'SeleniumBasic')
})

beforeAll(async () => {
  await loginPage.login('admin', 'admin')
})
beforeAll(async () => {
  // @ts-ignore
  let path = expect.getState().testPath
  path = path.substring(path.lastIndexOf('/') + 1)
  const execPageLink = await adhocPage.runBasicCommand('echo running test "' + path + '"', '.*')
  execPageHref = await execPageLink.getAttribute('href')
})

describe('execution', () => {
  it('default page load shows nodes view', async () => {
    await ctx.driver.get(execPageHref)

    await ctx.driver.wait(until.urlContains('/execution/show'), 25000)
    const execShowPage = new ExecutionShowPage(ctx, 'SeleniumBasic', '')

    // expect an execution state
    const display = await execShowPage.executionStateDisplay()
    const val = await display.getAttribute('data-execstate')
    expect(val.toUpperCase()).toBeDefined()

    // expect 'nodes' view to be active
    const viewContent = await execShowPage.viewContent('nodes')
    await ctx.driver.wait(until.elementIsVisible(viewContent))

    const displayed = await viewContent.isDisplayed()
    expect(displayed).toBeTruthy()

    // expect view dropdown to show nodes
    const viewDropdown = await execShowPage.viewDropdown()
    const viewDropdownText = await viewDropdown.getText()
    expect(viewDropdownText).toBe('View Nodes')

    // expect nodes button to be hidden
    const nodesButton = await execShowPage.viewButton('nodes')
    const btnDisplayed = await nodesButton.isDisplayed()
    expect(btnDisplayed).toBeFalsy()
    // expect other button to be shown
    const viewButton2 = await execShowPage.viewButton('output')
    const btnDisplayed2 = await viewButton2.isDisplayed()
    expect(btnDisplayed2).toBeTruthy()

  })

  it('fragment #output page load shows output view', async () => {
    await navigation.gotoProject('SeleniumBasic')
    await ctx.driver.wait(until.urlContains('/home'), 5000)

    await ctx.driver.get(execPageHref + '#output')

    await ctx.driver.wait(until.urlContains('/execution/show'), 25000)
    const execShowPage = new ExecutionShowPage(ctx, 'SeleniumBasic', '')

    // expect an execution state
    const display = await execShowPage.executionStateDisplay()
    const val = await display.getAttribute('data-execstate')
    expect(val.toUpperCase()).toBeDefined()

    // expect 'output' view to be active
    const viewContent = await execShowPage.viewContent('output')
    await ctx.driver.wait(until.elementIsVisible(viewContent))

    const displayed = await viewContent.isDisplayed()
    expect(displayed).toBeTruthy()

    // expect view dropdown to show correct text
    const viewDropdown = await execShowPage.viewDropdown()
    const viewDropdownText = await viewDropdown.getText()
    expect(viewDropdownText).toBe('View Log Output')

    // expect output button to be hidden
    const viewButton = await execShowPage.viewButton('output')
    const btnDisplayed = await viewButton.isDisplayed()
    expect(btnDisplayed).toBeFalsy()
    // expect other button to be shown
    const viewButton2 = await execShowPage.viewButton('nodes')
    const btnDisplayed2 = await viewButton2.isDisplayed()
    expect(btnDisplayed2).toBeTruthy()
  })

  it('output view toggle to nodes view with button', async () => {
    await navigation.gotoProject('SeleniumBasic')
    await ctx.driver.wait(until.urlContains('/home'), 5000)

    await ctx.driver.get(execPageHref + '#output')

    await ctx.driver.wait(until.urlContains('/execution/show'), 25000)
    const execShowPage = new ExecutionShowPage(ctx, 'SeleniumBasic', '')

    const toggleButton = await execShowPage.viewButton('nodes')
    const toggleBtnDisplayed = await toggleButton.isDisplayed()
    expect(toggleBtnDisplayed).toBeTruthy()

    await toggleButton.click()

    // expect 'nodes' view to be active
    const viewContent = await execShowPage.viewContent('nodes')
    await ctx.driver.wait(until.elementIsVisible(viewContent))

    const displayed = await viewContent.isDisplayed()
    expect(displayed).toBeTruthy()

    // expect view dropdown to show nodes
    const viewDropdown = await execShowPage.viewDropdown()
    const viewDropdownText = await viewDropdown.getText()
    expect(viewDropdownText).toBe('View Nodes')

    // expect nodes button to be hidden
    const nodesButton = await execShowPage.viewButton('nodes')
    const btnDisplayed = await nodesButton.isDisplayed()
    expect(btnDisplayed).toBeFalsy()
    // expect other button to be shown
    const viewButton2 = await execShowPage.viewButton('output')
    const btnDisplayed2 = await viewButton2.isDisplayed()
    expect(btnDisplayed2).toBeTruthy()

  })

  it('nodes view toggle to output view with button', async () => {
    await navigation.gotoProject('SeleniumBasic')
    await ctx.driver.wait(until.urlContains('/home'), 5000)

    await ctx.driver.get(execPageHref)

    await ctx.driver.wait(until.urlContains('/execution/show'), 25000)
    const execShowPage = new ExecutionShowPage(ctx, 'SeleniumBasic', '')

    const toggleButton = await execShowPage.viewButton('output')
    const toggleBtnDisplayed = await toggleButton.isDisplayed()
    expect(toggleBtnDisplayed).toBeTruthy()

    await toggleButton.click()

    // expect 'output' view to be active
    const viewContent = await execShowPage.viewContent('output')
    await ctx.driver.wait(until.elementIsVisible(viewContent))

    const displayed = await viewContent.isDisplayed()
    expect(displayed).toBeTruthy()

    // expect view dropdown to show correct text
    const viewDropdown = await execShowPage.viewDropdown()
    const viewDropdownText = await viewDropdown.getText()
    expect(viewDropdownText).toBe('View Log Output')

    // expect output button to be hidden
    const viewButton = await execShowPage.viewButton('output')
    const btnDisplayed = await viewButton.isDisplayed()
    expect(btnDisplayed).toBeFalsy()
    // expect other button to be shown
    const viewButton2 = await execShowPage.viewButton('nodes')
    const btnDisplayed2 = await viewButton2.isDisplayed()
    expect(btnDisplayed2).toBeTruthy()
  })

})
