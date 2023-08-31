import {RundeckClient} from '@rundeck/client'

import { RundeckVersion } from '../utilities/RundeckVersion'
import { Serial } from '../utilities/Async'

import {RootStore} from './RootStore'
import {ref} from "vue";

export class SystemStore {
    versionInfo: VersionInfo
    serverInfo?: ServerInfo
    appInfo: AppInfo

    loaded = ref<boolean>(false)

    constructor(readonly root: RootStore, readonly client: RundeckClient) {
        this.versionInfo = new VersionInfo()
        this.appInfo = new AppInfo()
    }

    loadMeta(meta: any) {
        if (meta.title) {
            this.appInfo.title = meta.title
        }
        if (meta.logocss) {
            this.appInfo.logocss = meta.logocss
        }
    }

    @Serial
    async load() {
        if (this.loaded.value)
            return

        const resp = await this.client.systemInfoGet()

        const verString = resp.system!.rundeckProperty!.version
        const ver = new RundeckVersion({versionString: verString})

        this.versionInfo.fromRundeckVersion(ver)
        this.serverInfo = new ServerInfo(
            resp.system!.rundeckProperty!.node!,
            resp.system!.rundeckProperty!.serverUUID!)

        this.loaded.value = true
    }
}

export class AppInfo {
    title = 'Rundeck'
    logocss = 'rdicon'
}

export class VersionInfo {
    full!: string
    number!: string
    tag!: string
    name!: string
    color!: string
    date!: Date
    icon!: string
    edition = 'Community'

    constructor() {}

    static FromRundeckVersion(ver: RundeckVersion): VersionInfo {
        const versionInfo = new VersionInfo
        return versionInfo.fromRundeckVersion(ver)
    }

    fromRundeckVersion(ver: RundeckVersion): VersionInfo {
        this.number = ver.versionSemantic()
        this.name = ver.versionName()
        this.icon = ver.versionIcon()
        this.color = ver.versionColor()
        this.tag = ver.data().tag

        return this
    }
}

export class ServerInfo {
    name!: string
    color!: string
    uuid!: string
    icon!: string

    constructor(name: string, uuid: string) {
        const ver = new RundeckVersion({})
        this.name = name
        this.uuid = uuid
        this.icon = ver.iconForVersion2(ver.splitUUID(uuid)['uuid0'])
        this.color = ver.splitUUID(uuid)['sixes'][0]
    }
}