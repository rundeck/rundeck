"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.CookieEnrichPolicy = exports.cookieEnrichPolicy = exports.runJobAndWait = exports.waitForExecutionComplete = exports.createWaitForRundeckReady = exports.waitForRundeckReady = void 0;
const ms_rest_js_1 = require("@azure/ms-rest-js");
const models_1 = require("@rundeck/client/dist/lib/models");
const util_1 = require("@rundeck/client/dist/util");
const util_2 = require("../async/util");
async function waitForRundeckReady(client, timeout = 500000) {
    await createWaitForRundeckReady(() => client, timeout);
}
exports.waitForRundeckReady = waitForRundeckReady;
async function createWaitForRundeckReady(factory, timeout = 500000) {
    const start = Date.now();
    const unauthMax = 10;
    const sleepTime = 2000;
    let unauthCount = 0;
    while (Date.now() - start < timeout) {
        try {
            const reqstart = Date.now();
            let resp = await factory().systemInfoGet();
            return;
        }
        catch (e) {
            if (e.statusCode === 403) {
                unauthCount++;
            }
            // if (e.statusCode) {
            //     console.debug(`Received status... ${e.statusCode}: ${e}`)
            // } else if(e.code){
            //     console.debug(`Waiting... ${e.code}: ${e}`)
            // } else {
            //     console.debug(`Waiting... ${Date.now() - start}`)
            // }
            if (unauthCount > unauthMax) {
                throw new Error(`Rundeck authentication failure: ${e}`);
            }
            await (0, util_2.sleep)(sleepTime);
        }
    }
    throw new Error('Timeout exceeded waiting for Rundeck to be ready.');
}
exports.createWaitForRundeckReady = createWaitForRundeckReady;
const STATUS_FINAL = [
    models_1.Status.Aborted,
    models_1.Status.Failed,
    models_1.Status.Succeeded,
    models_1.Status.Timedout
];
async function waitForExecutionComplete(client, id) {
    let resp;
    let curStatus = models_1.Status.Running;
    while (true) {
        resp = await client.executionStatusGet(id.toString());
        if (!resp.status) {
            throw new Error(`Unable to get execution status for ${id}`);
        }
        curStatus = resp.status;
        if (STATUS_FINAL.includes(curStatus))
            break;
        else
            await (0, util_2.sleep)(1000);
    }
    return resp;
}
exports.waitForExecutionComplete = waitForExecutionComplete;
async function runJobAndWait(client, id, options) {
    const resp = await client.jobExecutionRun(id, options);
    return await waitForExecutionComplete(client, resp.id);
}
exports.runJobAndWait = runJobAndWait;
function cookieEnrichPolicy(cookies) {
    return {
        create: (nextPolicy, options) => {
            return new CookieEnrichPolicy(nextPolicy, options, cookies);
        }
    };
}
exports.cookieEnrichPolicy = cookieEnrichPolicy;
/** Enriches each request with a set of cookies */
class CookieEnrichPolicy extends ms_rest_js_1.BaseRequestPolicy {
    constructor(nextPolicy, options, cookies) {
        super(nextPolicy, options);
        this.cookies = cookies;
    }
    async sendRequest(webResource) {
        const reqCookies = webResource.headers.get('cookie');
        const combinedCookies = (0, util_1.combineCookies)(reqCookies, this.cookies);
        webResource.headers.set('cookie', combinedCookies.join(';'));
        return await this._nextPolicy.sendRequest(webResource);
    }
}
exports.CookieEnrichPolicy = CookieEnrichPolicy;
//# sourceMappingURL=RundeckAPI.js.map