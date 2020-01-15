import {Context} from 'context'
import {CreateContext} from 'test/selenium'
import {LoginPage} from 'pages/login.page'
import {NavigationPage,Elems} from 'pages/navigation.page'
import {By, until} from 'selenium-webdriver'

import 'test/rundeck'
import { sleep } from 'async/util';

// We will initialize and cleanup in the before/after methods
let ctx: Context
let loginPage: LoginPage
let navigation: NavigationPage

beforeAll( async () => {
    ctx = await CreateContext()
    loginPage = new LoginPage(ctx)
    navigation = new NavigationPage(ctx)
})

beforeEach( async () => {
    ctx.currentTestName = expect.getState().currentTestName
    await ctx.screenSnap('initial')
})

afterAll( async () => {
    if (ctx)
        await ctx.dispose()
})

afterEach( async () => {
    await ctx.screenSnap('final')
})

describe('expanded navigation bar', () => {
    beforeAll(async () => {
        await loginPage.login('admin', 'admin')
        // await navigation.toggleSidebarExpand()
        // await navigation.freeze()
    })
    beforeEach(async ()=>{
        await navigation.gotoProject('SeleniumBasic')
        await ctx.driver.wait(until.urlContains('/home'), 5000)
    })
    it('visits jobs', async () => {
        await expect(ctx.driver.findElement(By.xpath(Elems.lnkJobs))).resolves.toBeDefined()
        await navigation.visitJobs()
        
        await ctx.driver.wait(until.urlContains('/jobs'), 5000)
        // const img = Buffer.from(await navigation.screenshot(true), 'base64')
        // expect(img).toMatchImageSnapshot({customSnapshotsDir: '__image_snapshots__', customDiffConfig: {threshold: 0.01}})
    })

    it('visits nodes', async () => {
        await navigation.visitNodes()
        await sleep(500)
        // const img = Buffer.from(await navigation.screenshot(true), 'base64')
        // expect(img).toMatchImageSnapshot({customSnapshotsDir: '__image_snapshots__', customDiffConfig: {threshold: 0.01}})
    })

    it('visits commands', async () => {
        await navigation.visitCommands()
        // const img = Buffer.from(await navigation.screenshot(true), 'base64')
        // expect(img).toMatchImageSnapshot({customSnapshotsDir: '__image_snapshots__', customDiffConfig: {threshold: 0.01}})
    })

    it('visits activity', async () => {
        await navigation.visitActivity()
        await navigation.blur()
        const elems = await navigation.ctx.driver.findElements(By.css('.fa-spinner'))

        // await Promise.all(elems.map(el => ctx.driver.wait(until.stalenessOf(el),5000)))
        // const img = Buffer.from(await navigation.screenshot(true), 'base64')
        // expect(img).toMatchImageSnapshot({customSnapshotsDir: '__image_snapshots__', customDiffConfig: {threshold: 0.01}})
    })

    it('visits System Configuration', async () => {
        await navigation.visitSystemConfiguration()
        await navigation.blur()
        await ctx.driver.findElement(By.xpath("//div[@class='alert alert-danger']"))
    })
})