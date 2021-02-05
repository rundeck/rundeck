import {RootStore} from './RootStore'
import {RundeckClient} from '@rundeck/client'
import { observable } from 'mobx'

export class NavBar {
    @observable items: Array<NavItem> = []

    constructor(readonly root: RootStore, readonly client: RundeckClient) {}
}

interface NavItem {
    id: string
    class: string
    label: string
}

interface NavLink extends NavItem {
    type: 'link'
    link: string
}

interface NavContainer extends NavItem {
    type: 'container'
}

