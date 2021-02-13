import {Context} from '@rundeck/testdeck/context'
import {CreateContext} from '@rundeck/testdeck/test/selenium'
import {ProjectCreatePage} from 'pages/projectCreate.page'
import {LoginPage} from 'pages/login.page'
import {EditNodesPage} from 'pages/editNodes.page'
import {until, By, Key} from 'selenium-webdriver'
import '@rundeck/testdeck/test/rundeck'

let ctx = CreateContext({projects: ['SeleniumBasic']})
let loginPage: LoginPage
let editNodesPage: EditNodesPage

beforeAll( async () => {
    loginPage = new LoginPage(ctx)
    editNodesPage = new EditNodesPage(ctx, 'SeleniumBasic')
})

beforeAll(async () => {
    await loginPage.login('admin', 'admin')
})

describe('edit nodes', () => {
    it('loads successfully', async () => {
        await editNodesPage.get();
        const src = await ctx.driver.getPageSource()
        const success = src.includes('Node Sources for the project')
        expect(success).toBe(true)
    })
})