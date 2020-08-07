import 'chromedriver'
import {Options} from 'selenium-webdriver/chrome'
import webdriver from 'selenium-webdriver'
import {toMatchImageSnapshot} from 'jest-image-snapshot'


import {Context} from '../context'
import {envOpts} from './rundeck'
import { IRequiredResources, TestProject } from '../TestProject'
import { rundeckPasswordAuth } from 'ts-rundeck'

import {CustomError} from '../util/Error'

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
        const driver = await new webdriver.Builder()
            .forBrowser('chrome')
            .setChromeOptions(opts)
            .build()

        const proxy = createDriverProxy(driver)

        return proxy
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

/**
 * WebDriver proxy that wraps calls in extra error handling.
 * In the case that a promise error is not caught(forgot await)
 * the stack trace will still include the original caller.
 */
function createDriverProxy(driver: webdriver.WebDriver) {
    return new Proxy<webdriver.WebDriver>(driver, {
        get: function(target, prop, receiver) {
            // @ts-ignore
            const orig = target[prop]

            if (orig == undefined)
                return

            if (typeof orig == 'function') {
                return function(...args: any[]) {
                    const error = new CustomError('Error calling selenium driver')
                    /**
                     * Remove the top of the stack so the first entry is the caller.
                     * Jest will properly print the test code lines instead of this function.
                     */
                    const stackArray = error.stack!.split("\n")
                    stackArray.splice(1,1)
                    error.stack = stackArray.join("\n")

                    const result = orig.apply(target, args)
                    if (typeof result.then !== 'undefined') {
                        return (async () => {
                            try {
                                const resolved = await result
                                return resolved
                            } catch(e) {
                                error.addCause(e)
                                throw error
                            }
                        })()
                    } else {
                        return result
                    }
                }
            } else {
                return orig
            }
        }
    })
}