import Path from 'path'

import { RundeckInstance } from "./RundeckCluster"
import { DockerCompose } from "./DockerCompose"

export interface IClusterManager {
    stopNode: (node: RundeckInstance) => Promise<void>
    startNode: (node: RundeckInstance) => Promise<void>
}

interface IConfig {
    licenseFile: string
    image: string
}

export class DockerClusterManager implements IClusterManager {
    compose: DockerCompose

    constructor(readonly dir: string, config: IConfig) {
        this.compose = new DockerCompose(dir, {
            env: {
                RUNDECK_LICENSE_FILE: Path.resolve(config.licenseFile),
                RUNDECK_IMAGE: config.image
            }
        })
    }

    async stopNode(node: RundeckInstance) {
        const {base} = node

        if (base.protocol != 'docker:')
            throw new Error(`Protocol not supported: ${base.protocol}`)

        const serviceName = base.hostname.split('_')[1]

        await this.compose.stop(serviceName)
    }

    async startNode(node: RundeckInstance) {
        const {base} = node

        if (base.protocol != 'docker:')
            throw new Error(`Protocol not supported: ${base.protocol}`)

        const serviceName = base.hostname.split('_')[1]

        await this.compose.start(serviceName)
    }

}