import 'chromedriver'
import {Options} from 'selenium-webdriver/chrome'
import webdriver from 'selenium-webdriver'
import {toMatchImageSnapshot} from 'jest-image-snapshot'


import {Context} from '../context'
import {envOpts} from './rundeck'
import { IRequiredResources, TestProject } from '../TestProject'
import { rundeckPasswordAuth } from 'ts-rundeck'

const opts = new Options()

jest.setTimeout(60000)

export function CreateContext(resources: IRequiredResources) {
    if (envOpts.TESTDECK_HEADLESS) {
        opts.addArguments('--headless', 'window-size=1192,870', '--no-sandbox')
    } else {
        opts.addArguments('window-size=1200,1000')
    }

    if (envOpts.TESTDECK_VISUAL_REGRESSION) {
        expect.extend({ toMatchImageSnapshot })
    } else {
        expect.extend({
            toMatchImageSnapshot: (received: any, ...actual: any[]) => {
                return {
                    message: () => 'NOOP',
                    pass: true
                }
            }
        })
    }

    opts.addArguments('--disable-rtc-smoothness-algorithm', '--disable-gpu-compositing', '--disable-gpu', '--force-device-scale-factor=1', '--disable-lcd-text', '--disable-dev-shm-usage')

    let driverProvider = async () => {
        return new webdriver.Builder()
            .forBrowser('chrome')
            .setChromeOptions(opts)
            .build()
    }

    let ctx = new Context(driverProvider, envOpts.TESTDECK_RUNDECK_URL, envOpts.TESTDECK_S3_UPLOAD, envOpts.TESTDECK_S3_BASE)

    /**
     * Configure before/after handlers common to all Selenium test suites
     */
    beforeAll( async () => {
        const client = rundeckPasswordAuth('admin', 'admin',{baseUri: envOpts.TESTDECK_RUNDECK_URL})
        await TestProject.LoadResources(client, resources)
        await ctx.init()
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

    return ctx
}
