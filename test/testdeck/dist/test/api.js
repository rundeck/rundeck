"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.CreateTestContext = exports.CreateRundeckCluster = void 0;
const url_1 = require("url");
const RundeckCluster_1 = require("../RundeckCluster");
const TestProject_1 = require("../TestProject");
const ts_rundeck_1 = require("@rundeck/client");
const RundeckAPI_1 = require("../util/RundeckAPI");
const ClusterManager_1 = require("../ClusterManager");
const rundeck_1 = require("./rundeck");
jest.setTimeout(60000);
async function CreateRundeckCluster() {
    const rundeckUrl = rundeck_1.envOpts.TESTDECK_RUNDECK_URL;
    const clusterManager = await ClusterManager_1.ClusterFactory.CreateCluster(rundeck_1.envOpts.TESTDECK_CLUSTER_CONFIG, {
        licenseFile: './license.key',
        image: rundeck_1.envOpts.TESTDECK_BASE_IMAGE
    });
    const cluster = new RundeckCluster_1.RundeckCluster(rundeckUrl, rundeck_1.envOpts.TESTDECK_RUNDECK_TOKEN ?
        new ts_rundeck_1.RundeckClient(new ts_rundeck_1.TokenCredentialProvider(rundeck_1.envOpts.TESTDECK_RUNDECK_TOKEN), { baseUri: rundeckUrl }) :
        (0, ts_rundeck_1.rundeckPasswordAuth)('admin', 'admin', {
            baseUri: rundeckUrl,
        }), clusterManager);
    const RundeckNodes = (await clusterManager.listNodes()).filter(u => /rundeck/.test(u.hostname));
    for (let [i, n] of RundeckNodes.entries()) {
        cluster.nodes.push(new RundeckCluster_1.RundeckInstance((0, url_1.parse)(`${n.href}/home/rundeck`), clientForBackend(rundeckUrl, rundeck_1.envOpts.TESTDECK_RUNDECK_TOKEN ?
            new ts_rundeck_1.TokenCredentialProvider(rundeck_1.envOpts.TESTDECK_RUNDECK_TOKEN) :
            new ts_rundeck_1.PasswordCredentialProvider(rundeckUrl, 'admin', 'admin'), `rundeck-${i + 1}`)));
    }
    return cluster;
}
exports.CreateRundeckCluster = CreateRundeckCluster;
function CreateTestContext(resources) {
    let context = { cluster: null };
    beforeAll(async () => {
        context.cluster = await CreateRundeckCluster();
        await (0, RundeckAPI_1.waitForRundeckReady)(context.cluster.client);
        await TestProject_1.TestProject.LoadResources(context.cluster.client, resources);
    });
    return context;
}
exports.CreateTestContext = CreateTestContext;
function clientForBackend(url, creds, backend) {
    const cookiePolicy = (0, RundeckAPI_1.cookieEnrichPolicy)([`backend=${backend}`]);
    return new ts_rundeck_1.RundeckClient(creds, {
        baseUri: url,
        noRetryPolicy: true,
        requestPolicyFactories: (factories) => factories.concat([cookiePolicy])
    });
}
//# sourceMappingURL=api.js.map