import {Context} from 'context'
import {CreateContext} from 'test/selenium'
import {LoginPage} from 'pages/login.page'
import {LogoutPage} from 'pages/logOut.page'
import {NavigationPage} from 'pages/navigation.page'
import {By,until} from 'selenium-webdriver'

import 'test/rundeck'
import { ProjectListPage } from 'pages/projectList.page'

// We will initialize and cleanup in the before/after methods
let ctx: Context
let loginPage: LoginPage
let navigation: NavigationPage
let projectList: ProjectListPage
let logoutPage: LogoutPage

beforeAll( async () => {
    ctx = await CreateContext()
    loginPage = new LoginPage(ctx)
    navigation = new NavigationPage(ctx)
    projectList = new ProjectListPage(ctx)
    logoutPage = new LogoutPage(ctx)
})

beforeEach( async () => {
    ctx.currentTestName = expect.getState().currentTestName
})

afterAll( async () => {
    if (ctx)
        await ctx.dispose()
})

// afterEach( async () => {
    // await ctx.screenSnap('final')
// })

it('Logs in through the GUI', async () => {
    await loginPage.get()
    await loginPage.login('admin', 'admin')
    // await navigation.toggleSidebarExpand()
    // const img = Buffer.from((await navigation.screenshot()), 'base64')
    //expect project list page
    let projCount = await projectList.getProjectsCount()
    let text = await projCount.getText()
    
    expect(text).toMatch(/\d+ Projects?/)
})

it('Logs in with bad password', async () => {
    await logoutPage.get()
    await loginPage.get()
    await loginPage.badLogin('admin', 'xxwrongpassword')
    
    //expect login error
    let loginpage = await ctx.driver.findElement(By.css("body#loginpage"))
    expect(loginpage).toBeDefined()

    let alert = await ctx.driver.findElement(By.css(".alert.alert-danger > span"))
    let value = await alert.getText()
    expect(value).toEqual('Invalid username and password.')

})
