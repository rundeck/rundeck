import {RundeckBrowser} from 'ts-rundeck'
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
    data:{[key:string]:any}
}

export interface RundeckToken {
    TOKEN: string
    URI: string
}
