import {RundeckBrowser} from '@rundeck/client'
import { NavItem } from '../stores/NavBar'
import { RootStore } from '../stores/RootStore'
import { EventBus } from "../utilities/vueEventBus";

export interface RundeckContext {
    eventBus: typeof EventBus
    rdBase: string
    apiVersion: string
    projectName: string
    activeTour: string
    activeTourStep: string
    appMeta: { [key:string]:any }
    token: RundeckToken
    tokens: {
        [key:string]: RundeckToken
    }
    rundeckClient: RundeckBrowser,
    data:{[key:string]:any},
    feature:{[key:string]: RundeckFeature}
    navbar: {
        items: Array<NavItem>
    }
    rootStore: RootStore
    locale?: string
    language?: string
}

export interface RundeckToken {
    TOKEN: string
    URI: string
}

export interface RundeckFeature {
    enabled: boolean
}
