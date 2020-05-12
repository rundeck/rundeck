import {Argv} from 'yargs'

import { ClusterFactory } from '../../ClusterManager'

import {Config} from '../../Config'

interface Opts {
    config: string
    image: string
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
            .option('image', {
                describe: 'The Rundeck Docker image to use instead of the default',
                type: 'string'
            })
    }

    async handler(opts: Opts) {
        const config = await Config.Load('./config.yml')
        const cluster = await ClusterFactory.CreateCluster(opts.config || config.clusterConfig, {
            image: opts.image || config.baseImage,
            licenseFile: config.licenseFile
        })

        await cluster.startCluster()
    }
}

module.exports = new ProvisionCommand()
