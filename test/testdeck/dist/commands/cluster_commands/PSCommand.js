"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const ClusterManager_1 = require("../../ClusterManager");
const Config_1 = require("../../Config");
class PSCommand {
    constructor() {
        this.command = "ps";
        this.describe = "List cluster instances";
    }
    builder(yargs) {
        return yargs
            .option('config', {
            describe: 'Cluster configuration location',
            type: 'string'
        });
    }
    async handler(opts) {
        const config = await Config_1.Config.Load('./config.yml', './config.user.yml');
        const cluster = await ClusterManager_1.ClusterFactory.CreateCluster(opts.config || config.clusterConfig, {
            image: config.baseImage,
            licenseFile: config.licenseFile
        });
        console.log(await cluster.listNodes());
    }
}
module.exports = new PSCommand();
//# sourceMappingURL=PSCommand.js.map