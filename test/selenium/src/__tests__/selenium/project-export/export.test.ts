import {Context} from '@rundeck/testdeck/context'
import {CreateContext} from '@rundeck/testdeck/test/selenium'
import {ProjectExportPage,Checkboxes,Radios, Elems} from 'pages/projectExport.page'
import {LoginPage} from 'pages/login.page'

import {until, By} from 'selenium-webdriver'
import '@rundeck/testdeck/test/rundeck'

// We will initialize and cleanup in the before/after methods
let ctx = CreateContext({projects: ['SeleniumBasic']})
let loginPage: LoginPage
let projectExportPage: ProjectExportPage

beforeAll( async () => {
    loginPage = new LoginPage(ctx)
    projectExportPage = new ProjectExportPage(ctx, 'SeleniumBasic')
    await loginPage.login('admin', 'admin')
})

describe('projectExport', () => {
    it('exports without errors', async () => {
        await projectExportPage.get()
        await projectExportPage.export()

        // Wait for download button to appear
        const dlBtn = await ctx.driver.findElement(Elems.downloadBtn)
        await ctx.driver.wait(until.elementIsVisible(dlBtn))

        // Ensure none of the "error" panels on the page have displayed
        const errorPanels = await ctx.driver.findElements(Elems.errorPanels)
        const visible = await Promise.all(errorPanels.map(el => el.isDisplayed()))
        expect(visible).not.toContain(true)
    })
})