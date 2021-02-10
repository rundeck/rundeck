import Vue from 'vue'
import {array ,object, withKnobs} from '@storybook/addon-knobs'

import NavBar from './NavBar.vue'

import {NavItem} from '../../stores/NavBar'
import {RootStore} from '../../stores/RootStore'

import {observable} from 'mobx'

export default {
    title: 'Navigation Bar',
    decorators: [withKnobs]
}

export const navBar = () => {
    const rootStore = new RootStore(window._rundeck.rundeckClient)

    rootStore.navBar.items = observable([
        {
            "type": "link",
            "id": "nav-rd-home",
            "group": "main",
            "class": "rdicon app-logo",
            "link": "/",
            "label": "",
            "visible": true,
        },
        {
            "type": "link",
            "id": "nav-project-dashboard-link",
            "group": "main",
            "class": "fas fa-clipboard-list",
            "link": "/project/test/home",
            "label": "Dashboard",
            "active": true,
            "visible": true,
        },
        {
            "type": "link",
            "id": "nav-jobs-link",
            "group": "main",
            "class": "fas fa-tasks",
            "link": "/project/test/jobs",
            "label": "Jobs",
            "visible": true,
        },
        {
            "type": "link",
            "id": "nav-nodes-link",
            "group": "main",
            "class": "fas fa-sitemap",
            "link": "/project/test/nodes",
            "label": "Nodes",
            "visible": true,
        },
        {
            "type": "link",
            "id": "nav-commands-link",
            "group": "main",
            "class": "fas fa-terminal",
            "link": "/project/test/command/run",
            "label": "Commands",
            "visible": true,
        },
        {
            "type": "link",
            "id": "nav-activity-link",
            "group": "main",
            "class": "fas fa-history",
            "link": "/project/test/activity",
            "label": "Activity",
            "visible": true,
        },
        {
            "type": "link",
            "id": "nav-schedules-link",
            "group": "main",
            "class": "fas fa-clock",
            "link": "/project/test/schedules",
            "label": "Schedules",
            "visible": true,
        },
        {
            "type": "link",
            "id": "nav-healthcheck-link",
            "group": "main",
            "class": "fas fa-heartbeat",
            "link": "/project/test/healthcheck",
            "label": "Health Checks",
            "visible": true,
        },
        {
            "type": "link",
            "id": "nav-tours-link",
            "group": "main",
            "class": "glyphicon glyphicon-question-sign",
            "link": "/project/test/tourmanager",
            "label": "Tour Manager",
            "visible": true,
        },
        {
            "type": "link",
            "id": "nav-calendars-link",
            "group": "main",
            "class": "fas fa-calendar-alt",
            "link": "/project/test/calendars",
            "label": "Calendars",
            "visible": true,
        },
        {
            "type": "link",
            "id": "nav-reactions-link",
            "group": "main",
            "class": "fas fa-plug",
            "link": "/rdproreactions/project/test/reactions",
            "label": "Reactions",
            "visible": true,
        },
        {
            "type": "link",
            "id": "nav-webhooks-link",
            "group": "main",
            "class": "fas fa-plug",
            "link": "/webhook/admin?project=test",
            "label": "Webhooks",
            "visible": true,
        },
        {
            "type": "container",
            "id": "nav-project-settings",
            "group": "bottom",
            "class": "fas fa-cogs",
            "label": "Project Settings",
            "visible": true,
        }
    ])
    
    return Vue.extend({
        components: { NavBar },
        template: '<div style="width: 65px; height: 100%;overflow: hidden;"><NavBar /></div>',
        mounted: function() {
            const el = this.$el as any
            el.parentNode.style.height = '100vh'
            el.parentNode.style.overflow = 'hidden'
            document.body.style.overflow = 'hidden'
        },
        provide: () => ({
            rootStore: rootStore,
        })
    })
}