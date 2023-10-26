import type {Meta, StoryFn} from '@storybook/vue3'

import UtilityBar from './UtilityBar.vue'
import RundeckInfo from '../widgets/rundeck-info/RundeckInfo.vue'
import {RootStore} from '../../stores/RootStore'
import {UtilityBarItem} from "../../stores/UtilityBar";
import { ServerInfo, VersionInfo } from '../../stores/System'
import {markRaw} from "vue";

export default {
    title: 'Utility Bar',
    component: UtilityBar,
} as Meta<typeof UtilityBar>

export const navBar: StoryFn<typeof UtilityBar> = () => {
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
            "widget": markRaw({
                props: {
                    version: {
                        default: version
                    },
                    server: {
                        default: server
                    }
                },
                components: {
                    RundeckInfo,
                },
                template: '<RundeckInfo app-info="" :server="server" latest="" :version="version"></RundeckInfo>'
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

    window._rundeck.rootStore = rootStore
    return {
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
    }
}

export const widgetCounter: StoryFn<typeof UtilityBar> = (args) => {
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

    return {
        setup() {
            return { ...args }
        },
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
        watch: {
            count(newVal, oldVal) {
                item.count = newVal
            }
        }
    }
}
widgetCounter.args = {
    count: 5,
}