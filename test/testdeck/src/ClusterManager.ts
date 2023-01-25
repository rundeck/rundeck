import Path from 'path'
import Url from 'url'

import { RundeckInstance } from "./RundeckCluster"
import { DockerCompose } from "./DockerCompose"

export interface IClusterManager {
    startCluster: () => Promise<void>
    stopCluster: () => Promise<void>
    stopNode: (node: RundeckInstance) => Promise<void>
    startNode: (node: RundeckInstance) => Promise<void>
    listNodes: () => Promise<Url.UrlWithStringQuery[]>
    logs: () => Promise<void>
}

interface IConfig {
    licenseFile: string
    image: string
    composeFileName?: string
}

const DEFAULT_DOCKER_COMPOSE_FILE_NAME = 'docker-compose.yml'

export class DockerClusterManager implements IClusterManager {
    compose: DockerCompose

    constructor(readonly dir: string, config: IConfig) {
        this.compose = new DockerCompose(
            dir,
     {
                env: {
                    RUNDECK_LICENSE_FILE: Path.resolve(config.licenseFile),
                    RUNDECK_IMAGE: config.image,
                    COMPOSE_PROJECT_NAME: 'testdeck'
                },
                composeFileName: config.composeFileName || DEFAULT_DOCKER_COMPOSE_FILE_NAME
            }
        )
    }

    async startCluster() {
        await this.compose.up()
    }

    async stopCluster() {
        await this.compose.stop()
    }

    async stopNode(node: RundeckInstance) {
        const {base} = node

        if (base.protocol != 'docker:')
            throw new Error(`Protocol not supported: ${base.protocol}`)

        const serviceName = base.hostname!.split('_')[1]

        await this.compose.stop(serviceName)
    }

    async startNode(node: RundeckInstance) {
        const {base} = node

        if (base.protocol != 'docker:')
            throw new Error(`Protocol not supported: ${base.protocol}`)

        const serviceName = base.hostname!.split('_')[1]

        await this.compose.start(serviceName)
    }

    async listNodes() {
        const containers = await this.compose.containers()

        return containers.map(c => Url.parse(`docker://${c}`))
    }

    async logs() {
        await this.compose.logs()
    }

}

export class ClusterFactory {
    static async CreateCluster(dir: string, config: IConfig): Promise<IClusterManager> {
        // TODO: Support non-docker clusters
        return new DockerClusterManager(dir, config)
    }
}