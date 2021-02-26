import Vue from 'vue'
import {array ,object, withKnobs} from '@storybook/addon-knobs'

import UtilityBar from './UtilityBar.vue'

import {RootStore} from '../../stores/RootStore'

import {observable} from 'mobx'
import { UtilityItem, UtilityActionItem } from 'src/stores/UtilityBar'

export default {
    title: 'Utility Bar',
    decorators: [withKnobs]
}

export const navBar = () => {
    const rootStore = new RootStore(window._rundeck.rundeckClient)

    rootStore.utilityBar.addItems([
        {
            "type": "action",
            "id": "utility-edition",
            "container": "root",
            "group": "left",
            "class": "rdicon app-logo",
            "label": "RUNDECK",
            "visible": true,
            "action": () => {alert('Clicked!')}
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
    ] as Array<UtilityActionItem>)
    
    return Vue.extend({
        components: { UtilityBar },
        template: '<div id="section-utility" style="width: 100%; height: 22px;"><UtilityBar /></div>',
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