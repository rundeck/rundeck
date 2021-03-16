import { action, computed, observable } from 'mobx'

import {RundeckClient} from '@rundeck/client'

import { RundeckVersion } from '../utilities/RundeckVersion'
import { Serial } from '../utilities/Async'

import {RootStore} from './RootStore'

export class SystemStore {
    @observable versionInfo: VersionInfo
    @observable serverInfo?: ServerInfo

    @observable loaded = false

    constructor(readonly root: RootStore, readonly client: RundeckClient) {
        this.versionInfo = new VersionInfo()
    }

    @Serial
    async load() {
        if (this.loaded)
            return

        const resp = await this.client.systemInfoGet()
        
        const verString = resp.system!.rundeckProperty!.version
        const ver = new RundeckVersion({versionString: verString})

        this.versionInfo.fromRundeckVersion(ver)
        this.serverInfo = new ServerInfo(
            resp.system!.rundeckProperty!.node!,
            resp.system!.rundeckProperty!.serverUUID!)

        this.loaded = true
    }
}

export class VersionInfo {
    @observable full!: string
    @observable number!: string
    @observable tag!: string
    @observable name!: string
    @observable color!: string
    @observable date!: Date
    @observable icon!: string
    @observable edition = 'Community'

    constructor() {}

    static FromRundeckVersion(ver: RundeckVersion): VersionInfo {
        const versionInfo = new VersionInfo
        return versionInfo.fromRundeckVersion(ver)
    }

    fromRundeckVersion(ver: RundeckVersion): VersionInfo {
        this.number = ver.versionSemantic(),
        this.name = ver.versionName(),
        this.icon = ver.versionIcon(),
        this.color = ver.versionColor()
        this.tag = ver.data().tag

        return this
    }
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