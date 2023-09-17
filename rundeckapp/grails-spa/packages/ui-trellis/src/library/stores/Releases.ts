import {RootStore} from './RootStore'
import {RundeckClient} from '@rundeck/client'

import axios from 'axios'
import { VersionInfo } from './System'
import { Serial } from '../utilities/Async'

export class Releases {
    releases: Array<Release> = []

    constructor(readonly root: RootStore, readonly client: RundeckClient) {}

    @Serial
    async load() {
        if (this.releases.length >= 1) {
            return
        }
        const results = await axios.get<Array<ApiRelease>>('https://api.rundeck.com/news/v1/release')
        results.data.forEach(r => {
            this.releases.push(Release.FromApi(r))
        })
    }
}

export class Release extends VersionInfo {
    static FromApi(resp: ApiRelease): Release {
        const release = new Release

        const ver = resp.version

        release.full = resp.name
        release.icon = ver.icon
        release.tag = ver.tag
        release.name = ver.name
        release.color = ver.color
        release.number = `${ver.major}.${ver.minor}.${ver.patch}`
        release.date = new Date(Date.parse(ver.date))

        return release
    }
}

interface ApiRelease {
    name: string
    version: {
        major: number
        minor: number
        patch: number
        tag: string
        date: string
        color: string
        name: string
        icon: string
    }
}
