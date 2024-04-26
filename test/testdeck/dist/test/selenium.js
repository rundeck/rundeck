"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.CreateContext = void 0;
require("chromedriver");
const chrome_1 = require("selenium-webdriver/chrome");
const selenium_webdriver_1 = __importDefault(require("selenium-webdriver"));
const jest_image_snapshot_1 = require("jest-image-snapshot");
const context_1 = require("../context");
const rundeck_1 = require("./rundeck");
const TestProject_1 = require("../TestProject");
const ts_rundeck_1 = require("@rundeck/client");
const Error_1 = require("../util/Error");
const opts = new chrome_1.Options();
jest.setTimeout(60000);
function CreateContext(resources) {
    if (rundeck_1.envOpts.TESTDECK_HEADLESS) {
        opts.addArguments('--headless', 'window-size=1192,870', '--no-sandbox');
    }
    else {
        opts.addArguments('window-size=1200,1000');
    }
    if (rundeck_1.envOpts.TESTDECK_VISUAL_REGRESSION) {
        expect.extend({ toMatchImageSnapshot: jest_image_snapshot_1.toMatchImageSnapshot });
    }
    else {
        expect.extend({
            toMatchImageSnapshot: (received, ...actual) => {
                return {
                    message: () => 'NOOP',
                    pass: true
                };
            }
        });
    }
    opts.addArguments('--disable-rtc-smoothness-algorithm', '--disable-gpu-compositing', '--disable-gpu', '--force-device-scale-factor=1', '--disable-lcd-text', '--disable-dev-shm-usage');
    let driverProvider = async () => {
        try {
            const driver = await new selenium_webdriver_1.default.Builder()
                .forBrowser('chrome')
                .setChromeOptions(opts)
                .build();
            const proxy = createDriverProxy(driver);
            return proxy;
        }
        catch (e) {
            console.log(`Error creating webdriver: ${e}`, e);
        }
    };
    let ctx = new context_1.Context(driverProvider, rundeck_1.envOpts.TESTDECK_RUNDECK_URL, rundeck_1.envOpts.TESTDECK_S3_UPLOAD, rundeck_1.envOpts.TESTDECK_S3_BASE);
    /**
     * Configure before/after handlers common to all Selenium test suites
     */
    beforeAll(async () => {
        const client = rundeck_1.envOpts.TESTDECK_RUNDECK_TOKEN ?
            new ts_rundeck_1.RundeckClient(new ts_rundeck_1.TokenCredentialProvider(rundeck_1.envOpts.TESTDECK_RUNDECK_TOKEN), { baseUri: rundeck_1.envOpts.TESTDECK_RUNDECK_URL }) :
            (0, ts_rundeck_1.rundeckPasswordAuth)('admin', 'admin', { baseUri: rundeck_1.envOpts.TESTDECK_RUNDECK_URL });
        try {
            await TestProject_1.TestProject.LoadResources(client, resources);
        }
        catch (e) {
            console.log(`Error in TestProject.LoadResources: ${e}`, e);
        }
        await ctx.init();
    });
    beforeEach(async () => {
        ctx.currentTestName = expect.getState().currentTestName;
        await ctx.screenSnap('initial');
    });
    afterAll(async () => {
        if (ctx)
            await ctx.dispose();
    });
    afterEach(async () => {
        await ctx.screenSnap('final');
    });
    return ctx;
}
exports.CreateContext = CreateContext;
/**
 * WebDriver proxy that wraps calls in extra error handling.
 * In the case that a promise error is not caught(forgot await)
 * the stack trace will still include the original caller.
 */
function createDriverProxy(driver) {
    return new Proxy(driver, {
        get: function (target, prop, receiver) {
            // @ts-ignore
            const orig = target[prop];
            if (orig == undefined)
                return;
            if (typeof orig == 'function') {
                return function (...args) {
                    const error = new Error_1.CustomError('Error calling selenium driver');
                    /**
                     * Remove the top of the stack so the first entry is the caller.
                     * Jest will properly print the test code lines instead of this function.
                     */
                    const stackArray = error.stack.split("\n");
                    stackArray.splice(1, 1);
                    error.stack = stackArray.join("\n");
                    const result = orig.apply(target, args);
                    if (typeof result.then !== 'undefined') {
                        return (async () => {
                            try {
                                const resolved = await result;
                                return resolved;
                            }
                            catch (e) {
                                error.addCause(e);
                                throw error;
                            }
                        })();
                    }
                    else {
                        return result;
                    }
                };
            }
            else {
                return orig;
            }
        }
    });
}
//# sourceMappingURL=selenium.js.map