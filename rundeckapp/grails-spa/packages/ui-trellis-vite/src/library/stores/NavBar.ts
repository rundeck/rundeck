import {RootStore} from './RootStore'
import {RundeckClient} from '@rundeck/client'
import { action, computed, observable } from 'mobx'

export class NavBar {
    @observable items: Array<NavItem> = []

    @observable overflow: Array<NavItem> = []

    overflowItem: NavContainer = {
        id: 'overflow',
        priority: 0,
        type: 'container',
        style: 'list',
        group: 'bottom',
        class: 'fas fa-ellipsis-h',
        label: 'More',
        visible: false,
    }

    constructor(readonly root: RootStore, readonly client: RundeckClient) {
        if (window._rundeck.navbar) {
            window._rundeck.navbar.items.forEach(i => {
                this.items.push({...i, visible: true, container: i.container || 'root'})
            })
        }
    }

    /** Returns items in proper sort order */
    getItems() {
        return this.items.slice().sort((a, b) => a.priority - b.priority)
    }

    @action
    addItems(items: Array<NavItem>) {
        items.forEach(i => this.items.push(i))
        this.items.sort((a, b) => a.priority - b.priority)
    }

    containerGroupItems(container: string, group: string) {
            const items = this.getItems().filter(i => i.group == group && i.container == container)
            return items
    }

    containerItems(container: string) {
        const items = this.getItems().filter(i => i.container == container)

        return items
    }

    groupItems(group: string) {
        const items = this.getItems().filter(i => i.group == group)
        return items
    }

    @computed
    get isOverflowing() {
        return this.items.some(i => i.container == 'overflow')
    }

    @computed
    get visibleItems(): Array<NavItem> {
        return this.items
    }

    @action
    overflowOne() {
        const candidate = this.containerGroupItems('root', 'main').slice().reverse().shift()

        if (candidate)
            candidate.container = 'overflow'
    }

    @action
    showOne() {
        const candidate = this.containerGroupItems('overflow', 'main').slice().shift()

        if (candidate)
            candidate.container = 'root'
    }
}

export interface NavItem {
    id: string
    priority: number
    class?: string
    label?: string
    container?: string
    group?: string
    visible: boolean
}

export interface NavLink extends NavItem {
    type: 'link'
    link: string
}

export interface NavContainer extends NavItem {
    type: 'container'
    style: 'icon' | 'list'
}
