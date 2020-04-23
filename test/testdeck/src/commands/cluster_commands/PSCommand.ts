import {Argv} from 'yargs'

import { ClusterFactory } from '../../ClusterManager'

import {Config} from '../../Config'

interface Opts {
    config: string
}

class PSCommand {
    command = "ps"
    describe = "List cluster instances"

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

        console.log(await cluster.listNodes())
    }
}

module.exports = new PSCommand()
