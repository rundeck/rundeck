import {Context} from 'context'
import {CreateContext} from 'test/selenium'
import {ProjectCreatePage} from 'pages/projectCreate.page'
import {LoginPage} from 'pages/login.page'
import {JobCreatePage} from 'pages/jobCreate.page'
import {until} from 'selenium-webdriver'
import {sleep} from 'async/util';
import 'test/rundeck'

// We will initialize and cleanup in the before/after methods
let ctx: Context
let loginPage: LoginPage
let projectCreate: ProjectCreatePage
let jobCreatePage: JobCreatePage

beforeAll( async () => {
    ctx = await CreateContext()
    loginPage = new LoginPage(ctx)
    projectCreate = new ProjectCreatePage(ctx)
    jobCreatePage = new JobCreatePage(ctx, 'SeleniumBasic')
})

beforeEach( async () => {
    ctx.currentTestName = expect.getState().currentTestName
})

afterAll( async () => {
    if (ctx)
        await ctx.dispose()
})

// afterEach( async () => {
//     await ctx.screenSnap('final')
// })

beforeAll(async () => {
    await loginPage.login('admin', 'admin')
})

describe('project', () => {
    // it('has not visually regressed', async () => {
    //     await projectCreate.get()
    //     const img = Buffer.from(await projectCreate.screenshot(true), 'base64')
    //     expect(img).toMatchImageSnapshot({customSnapshotsDir: '__image_snapshots__', customDiffConfig: {threshold: 0.01}})
    // })
    it('has basic fields', async () => {
        await projectCreate.get()
        await ctx.driver.wait(until.urlContains('/resources/createProject'), 5000)
        expect(projectCreate.projectNameInput()).toBeDefined()
        expect(projectCreate.labelInput()).toBeDefined()
        expect(projectCreate.descriptionInput()).toBeDefined()
    })
})

describe('job', () => {
    it('has basic fields', async () => {
        await jobCreatePage.get()
        await ctx.driver.wait(until.urlContains('/job/create'), 5000)
        expect(jobCreatePage.jobNameInput()).toBeDefined()
        expect(jobCreatePage.groupPathInput()).toBeDefined()
        expect(jobCreatePage.descriptionTextarea()).toBeDefined()
        
    })
    // it('has not visually regressed', async () => {
    //     await jobCreatePage.get()
    //     const img = Buffer.from(await jobCreatePage.screenshot(true), 'base64')
    //     expect(img).toMatchImageSnapshot({customSnapshotsDir: '__image_snapshots__', customDiffConfig: {threshold: 0.01}})
    // })
})