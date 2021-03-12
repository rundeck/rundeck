import {RootStore} from './RootStore'
import {RundeckClient} from '@rundeck/client'

import axios from 'axios'

export class Releases {
    constructor(readonly root: RootStore, readonly client: RundeckClient) {}

    async load() {
        const results = await axios.get('https://api.rundeck.com/news/v1/release')
    }
}

interface Release {
    name: string
    version: {
        major: number
        minor: number
        patch: number
        tag: string
        date: Date
        color: string
        name: string
        icon: string
    }
}
