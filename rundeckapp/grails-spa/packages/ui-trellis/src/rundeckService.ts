import { URL } from 'url'
import {RundeckToken} from './interfaces/rundeckWindow'
import {AppLinks} from './interfaces/AppLinks'

export function getRundeckContext() {
    return window._rundeck
}

export function getAppLinks() {
    return window.appLinks
}

export function getSynchronizerToken() {
    const tokenString = document.getElementById('web_ui_token')!.innerText
    return JSON.parse(tokenString) as RundeckToken
}

/**
 * Generate link by joining with the Rundeck base path
 */
export function url(path: string) {
    const context = getRundeckContext()
    return new window.URL(path, context.rdBase)
}