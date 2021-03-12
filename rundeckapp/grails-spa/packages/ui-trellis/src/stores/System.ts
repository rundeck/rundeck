import {RootStore} from './RootStore'
import {RundeckClient} from '@rundeck/client'
import { action, computed, observable } from 'mobx'

import { RundeckVersion } from '../utilities/RundeckVersion'

export class SystemStore {
    @observable versionInfo?: VersionInfo
    @observable serverInfo?: ServerInfo

    constructor(readonly root: RootStore, readonly client: RundeckClient) {}

    async load() {
        console.log('System')
        const resp = await this.client.systemInfoGet()
        
        const verString = resp.system!.rundeckProperty!.version
        console.log(verString)
        const ver = new RundeckVersion({versionString: verString})

        const versionInfo = new VersionInfo()
        
        versionInfo.number = ver.versionSemantic(),
        versionInfo.name = ver.versionName(),
        versionInfo.icon = ver.versionIcon(),
        versionInfo.color = ver.versionColor()
        versionInfo.tag = ver.data().tag

        console.log(ver.data())

        this.versionInfo = versionInfo
        this.serverInfo = new ServerInfo(
            resp.system!.rundeckProperty!.node!,
            resp.system!.rundeckProperty!.serverUUID!)
    }
}

export class VersionInfo {
    @observable number!: string
    @observable tag!: string
    @observable name!: string
    @observable color!: string
    @observable date!: Date
    @observable icon!: string

    constructor() {}
}

export class ServerInfo {
    name!: string
    color!: number
    uuid!: string
    icon!: string

    constructor(name: string, uuid: string) {
        const ver = new RundeckVersion({})
        this.name = name
        this.uuid = uuid
        this.icon = ver.iconForVersion2(ver.splitUUID('f1dbb7ed-c575-4154-8d01-216a59d7cb5e')['uuid4'])
        this.color = ver.splitUUID('f1dbb7ed-c575-4154-8d01-216a59d7cb5e')['uuid0']
    }
}