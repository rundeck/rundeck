import 'chromedriver'
import {Options} from 'selenium-webdriver/chrome'
import webdriver from 'selenium-webdriver'
import {toMatchImageSnapshot} from 'jest-image-snapshot'


import {Context} from 'context'
import {ParseBool} from 'util/parseBool'

const opts = new Options()

jest.setTimeout(60000)

const envOpts = {
    RUNDECK_URL: process.env.RUNDECK_URL || 'http://127.0.0.1:4440',
    CI: ParseBool(process.env.CI),
    HEADLESS: ParseBool(process.env.HEADLESS) || ParseBool(process.env.CI),
    S3_UPLOAD: ParseBool(process.env.S3_UPLOAD) || ParseBool(process.env.CI),
    S3_BASE: process.env.S3_BASE,
}

export async function CreateContext() {
    if (envOpts.HEADLESS) {
        opts.addArguments('--headless', 'window-size=1192,870', '--no-sandbox')
        expect.extend({ toMatchImageSnapshot })
    }
    else {
        opts.addArguments('window-size=1200,1000')
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

    let driver = await new webdriver.Builder()
        .forBrowser('chrome')
        .setChromeOptions(opts)
        .build()

    let ctx = new Context(driver, envOpts.RUNDECK_URL, envOpts.S3_UPLOAD, envOpts.S3_BASE)

    return ctx
}
