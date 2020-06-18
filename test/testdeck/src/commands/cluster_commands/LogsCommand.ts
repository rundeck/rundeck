import {Argv} from 'yargs'

import { ClusterFactory } from '../../ClusterManager'

import {Config} from '../../Config'

interface Opts {
    config: string
    image: string
}

class LogsCommand {
    command = "logs"
    describe = "Print cluster logs"

    builder(yargs: Argv) {
        return yargs
            .option('config', {
                describe: 'Cluster configuration location',
                type: 'string'
            })
    }

    async handler(opts: Opts) {
        const config = await Config.Load('./config.yml')

        const clusterConfig = {
            image: opts.image || config.baseImage,
            licenseFile: config.licenseFile
        }

        const cluster = await ClusterFactory.CreateCluster(opts.config || config.clusterConfig, clusterConfig)

        await cluster.logs()
    }
}

module.exports = new LogsCommand()
