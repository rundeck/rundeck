import {Argv} from 'yargs'

import { ClusterFactory } from '../../ClusterManager'

import {Config} from '../../Config'

interface Opts {
    config: string
}

class ProvisionCommand {
    command = "provision"
    describe = "Provision a cluster"

    builder(yargs: Argv) {
        return yargs
            .option('config', {
                describe: 'Cluster configuration location',
                type: 'string'
            })
    }

    async handler(opts: Opts) {
        const config = await Config.Load('./config.yml')
        const cluster = await ClusterFactory.CreateCluster(opts.config || config.clusterConfig, {
            image: config.baseImage,
            licenseFile: config.licenseFile
        })

        await cluster.startCluster()
    }
}

module.exports = new ProvisionCommand()
