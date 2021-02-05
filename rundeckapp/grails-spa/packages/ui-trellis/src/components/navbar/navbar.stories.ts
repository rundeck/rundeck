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
            "class": "rdicon app-logo",
            "link": "/",
            "label": "",
        },
        {
            "type": "link",
            "id": "nav-project-dashboard-link",
            "class": "fas fa-clipboard-list",
            "link": "/project/test/home",
            "label": "Dashboard",
            "active": true
        },
        {
            "type": "link",
            "id": "nav-jobs-link",
            "class": "fas fa-tasks",
            "link": "/project/test/jobs",
            "label": "Jobs"
        },
        {
            "type": "link",
            "id": "nav-nodes-link",
            "class": "fas fa-sitemap",
            "link": "/project/test/nodes",
            "label": "Nodes"
        },
        {
            "type": "link",
            "id": "nav-commands-link",
            "class": "fas fa-terminal",
            "link": "/project/test/command/run",
            "label": "Commands"
        },
        {
            "type": "link",
            "id": "nav-activity-link",
            "class": "fas fa-history",
            "link": "/project/test/activity",
            "label": "Activity"
        },
        {
            "type": "link",
            "id": "",
            "class": "fas fa-clock",
            "link": "/project/test/schedules",
            "label": "Schedules"
        },
        {
            "type": "link",
            "id": "",
            "class": "fas fa-heartbeat",
            "link": "/project/test/healthcheck",
            "label": "Health Checks"
        },
        {
            "type": "link",
            "id": "",
            "class": "glyphicon glyphicon-question-sign",
            "link": "/project/test/tourmanager",
            "label": "Tour Manager"
        },
        {
            "type": "link",
            "id": "",
            "class": "fas fa-calendar-alt",
            "link": "/project/test/calendars",
            "label": "Calendars"
        },
        {
            "type": "link",
            "id": "",
            "class": "fas fa-plug",
            "link": "/rdproreactions/project/test/reactions",
            "label": "Reactions"
        },
        {
            "type": "link",
            "id": "",
            "class": "fas fa-plug",
            "link": "/webhook/admin?project=test",
            "label": "Webhooks"
        },
        {
            "type": "container",
            "id": "nav-project-settings",
            "class": "fas fa-cogs",
            "label": "Project Settings"
        }
    ])
    
    return Vue.extend({
        components: { NavBar },
        template: '<div style="width: 65px; height: 100%"><NavBar /></div>',
        mounted: function() {
            const el = this.$el as any
            el.parentNode.style.height = '100vh'
            el.parentNode.style.overflowY = 'hidden'
        },
        provide: () => ({
            rootStore: rootStore,
        })
    })
}