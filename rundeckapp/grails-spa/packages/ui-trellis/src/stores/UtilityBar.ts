import {RootStore} from './RootStore'
import {RundeckClient} from '@rundeck/client'
import { action, computed, observable } from 'mobx'
import { VueConstructor } from 'vue'

export class UtilityBar {
    @observable items: Array<UtilityItem> = []

    @observable overflow: Array<UtilityItem> = []

    constructor(readonly root: RootStore, readonly client: RundeckClient) {
        if (window._rundeck.navbar) {
            window._rundeck.navbar.items.forEach(i => {
                this.items.push({...i, visible: true, container: i.container || 'root'})
            })
        }
    }

    getItem(id: string) {
        return this.items.find(i => i.id == id) as UtilityBarItem
    }

    addOrReplaceItem(item: UtilityBarItem) {
        const existing = this.items.find(i => i.id == item.id)
        if (existing) {
            const index = this.items.indexOf(existing)
            this.items.splice(index, 1, item)
        }
        else {
            this.items.push(item)
        }
    }

    addItems(items: Array<UtilityBarItem>) {
        items.forEach(i => this.items.push(i))
    }

    containerGroupItems(container: string, group: string) {
        const items = this.items.filter(i => i.group == group && i.container == container)
        return items
    }

    containerItems(container: string) {
        const items = this.items.filter(i => i.container == container)

        return items
    }

    groupItems(group: string) {
        const items = this.items.filter(i => i.group == group)
        return items
    }
}


export type UtilityBarItem = UtilityWidgetItem | UtilityActionItem

export interface UtilityItem {
    id: string
    class?: string
    label?: string
    container?: string
    group?: string
    visible: boolean
    count?: number
}

export interface UtilityActionItem extends UtilityItem {
    type: 'action',
    action: () => void
}

export interface UtilityWidgetItem extends UtilityItem {
    type: 'widget'
    widget: VueConstructor
}