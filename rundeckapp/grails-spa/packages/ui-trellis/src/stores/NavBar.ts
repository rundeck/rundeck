import {RootStore} from './RootStore'
import {RundeckClient} from '@rundeck/client'
import { observable } from 'mobx'

export class NavBar {
    @observable items: Array<NavItem> = []

    constructor(readonly root: RootStore, readonly client: RundeckClient) {}
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

