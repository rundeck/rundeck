import {RundeckBrowser} from '@rundeck/client'
import { NavItem } from '../stores/NavBar'
import Vue from 'vue'

export interface RundeckContext {
    eventBus: Vue
    rdBase: string
    apiVersion: string
    projectName: string
    activeTour: string
    activeTourStep: string
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
}

export interface RundeckToken {
    TOKEN: string
    URI: string
}

export interface RundeckFeature {
    enabled: boolean
}
