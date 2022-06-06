import Vue from 'vue'

import UtilityBar from './UtilityBar.vue'
import RundeckInfo from '../widgets/rundeck-info/RundeckInfo.vue'

import {RootStore} from '../../stores/RootStore'

import { UtilityBarItem } from 'src/stores/UtilityBar'
import { ServerInfo, VersionInfo } from '../../stores/System'

export default {
    title: 'Utility Bar'
}

export const navBar = () => {
    const rootStore = new RootStore(window._rundeck.rundeckClient)
    const server = new ServerInfo('xubuntu', 'f1dbb7ed-c575-4154-8d01-216a59d7cb5e')

    const version = new VersionInfo

    version.number = '3.4.0'
    version.name = 'Papadum'
    version.icon = 'book'
    version.color = 'aquamarine'

    rootStore.utilityBar.addItems([
        {
            "type": "widget",
            "id": "utility-edition",
            "container": "root",
            "group": "left",
            "class": "rdicon app-logo",
            "label": "ENTERPRISE 3.4.0",
            "visible": true,
            "widget": RundeckInfo.extend({
                props: {
                    version: {
                        default: version
                    },
                    server: {
                        default: server
                    }
                }
            })
        },
        {
            "type": "action",
            "id": "utility-instance",
            "container": "root",
            "group": "left",
            "class": "fas fa-glass-martini fas-xs",
            "label": "ec554baf55",
            "visible": true,
            "action": () => {alert('Clicked!')}
        },
        {
            "type": "action",
            "id": "utility-support",
            "container": "root",
            "group": "right",
            "class": "fas fa-question-circle fas-xs",
            "label": "Support",
            "visible": true,
            "action": () => {alert('Support!')}
        },
        {
            "type": "action",
            "id": "utility-tours",
            "container": "root",
            "group": "right",
            "class": "fas fa-lightbulb",
            "label": "Tours",
            "visible": true,
            "action": () => {alert('Tours!')}
        }
    ] as Array<UtilityBarItem>)

    return Vue.extend({
        components: { UtilityBar },
        template: '<div style="display: flex; flex-direction: column-reverse; height: 100%"><div id="section-utility" style="width: 100%; height: 22px;"><UtilityBar /></div></div>',
        mounted: function() {
            const el = this.$el as any
            el.parentNode.style.height = '100vh'
            el.parentNode.style.overflow = 'hidden'
            el.parentNode.style.position = 'relative'
            document.body.style.overflow = 'hidden'
        },
        provide: () => ({
            rootStore: rootStore,
        })
    })
}

export const widgetCounter = () => {
    const rootStore = new RootStore(window._rundeck.rundeckClient)

    rootStore.utilityBar.addItems([
    {
        "type": "action",
        "id": "utility-instance",
        "container": "root",
        "group": "left",
        "class": "fas fa-glass-martini fas-xs",
        "label": "ec554baf55",
        "visible": true,
        "action": () => {alert('Clicked!')},
        count: 5
    }])

    const item = rootStore.utilityBar.getItem('utility-instance')

    return Vue.extend({
        components: { UtilityBar },
        template: '<div style="display: flex; flex-direction: column-reverse; height: 100%"><div id="section-utility" style="width: 100%; height: 22px;"><UtilityBar /></div></div>',
        mounted: function() {
            const el = this.$el as any
            el.parentNode.style.height = '100vh'
            el.parentNode.style.overflow = 'hidden'
            el.parentNode.style.position = 'relative'
            document.body.style.overflow = 'hidden'
        },
        provide: () => ({
            rootStore: rootStore,
        }),
        props: {
            count: {default:  5}
        },
        watch: {
            count(newVal, oldVal) {
                item.count = newVal
            }
        }
    })
}