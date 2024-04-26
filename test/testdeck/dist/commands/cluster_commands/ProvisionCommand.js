"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const ClusterManager_1 = require("../../ClusterManager");
const Config_1 = require("../../Config");
class ProvisionCommand {
    constructor() {
        this.command = "provision";
        this.describe = "Provision a cluster";
    }
    builder(yargs) {
        return yargs
            .option('config', {
            describe: 'Cluster configuration location',
            type: 'string'
        })
            .option('image', {
            describe: 'The Rundeck Docker image to use instead of the default',
            type: 'string'
        });
    }
    async handler(opts) {
        const config = await Config_1.Config.Load('./config.yml', './config.user.yml');
        const clusterConfig = {
            image: opts.image || config.baseImage,
            licenseFile: config.licenseFile
        };
        console.log(clusterConfig);
        const cluster = await ClusterManager_1.ClusterFactory.CreateCluster(opts.config || config.clusterConfig, clusterConfig);
        await cluster.startCluster();
    }
}
module.exports = new ProvisionCommand();
//# sourceMappingURL=ProvisionCommand.js.map