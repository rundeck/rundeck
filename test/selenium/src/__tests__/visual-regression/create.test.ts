import {Context} from 'context'
import {CreateContext} from 'test/selenium'
import {ProjectCreatePage} from 'pages/projectCreate.page'
import {LoginPage} from 'pages/login.page'
import {JobCreatePage} from 'pages/jobCreate.page'

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
    await ctx.dispose()
})

afterEach( async () => {
    await ctx.screenSnap('final')
})

beforeAll(async () => {
    await loginPage.login('admin', 'admin')
})

describe('project', () => {
    it('has not visually regressed', async () => {
        await projectCreate.get()
        const img = Buffer.from(await projectCreate.screenshot(true), 'base64')
        expect(img).toMatchImageSnapshot({customSnapshotsDir: '__image_snapshots__', customDiffConfig: {threshold: 0.01}})
    })
})

describe('job', () => {
    it('has not visually regressed', async () => {
        await jobCreatePage.get()
        const img = Buffer.from(await jobCreatePage.screenshot(true), 'base64')
        expect(img).toMatchImageSnapshot({customSnapshotsDir: '__image_snapshots__', customDiffConfig: {threshold: 0.01}})
    })
})