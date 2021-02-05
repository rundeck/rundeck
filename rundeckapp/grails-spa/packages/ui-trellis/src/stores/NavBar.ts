import {RootStore} from './RootStore'
import {RundeckClient} from '@rundeck/client'
import { observable } from 'mobx'

export class NavBar {
    @observable items: Array<NavItem> = []

    constructor(readonly root: RootStore, readonly client: RundeckClient) {
        if (window._rundeck?.navbar) {
            window._rundeck.navbar.items.forEach(i => {
                this.items.push(i)
            })
        }
    }
}

export interface NavItem {
    id: string
    class?: string
    label?: string
}

export interface NavLink extends NavItem {
    type: 'link'
    link: string
}

export interface NavContainer extends NavItem {
    type: 'container'
}

