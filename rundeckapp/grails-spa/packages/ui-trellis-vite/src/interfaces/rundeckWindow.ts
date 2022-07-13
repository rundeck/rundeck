import {RundeckBrowser} from '@rundeck/client'
import { NavItem } from '../stores/NavBar'
import Vue from 'vue'
import { RootStore } from '../stores/RootStore'

export interface RundeckContext {
    eventBus: Vue
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
}

export interface RundeckToken {
    TOKEN: string
    URI: string
}

export interface RundeckFeature {
    enabled: boolean
}
