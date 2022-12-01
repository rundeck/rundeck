import Vue from 'vue'

import NavBar from './NavBar.vue'

import {NavItem, NavLink} from '../../stores/NavBar'
import {RootStore} from '../../stores/RootStore'

import {observable} from 'mobx'

export default {
    title: 'Navigation Bar'
}

export const navBar = () => {
    const rootStore = new RootStore(window._rundeck.rundeckClient)

    rootStore.navBar.addItems([
        {
            "type": "link",
            "id": "nav-project-dashboard-link",
            priority: 0,
            "container": "root",
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
            priority: 0,
            "container": "root",
            "group": "main",
            "class": "fas fa-tasks",
            "link": "/project/test/jobs",
            "label": "Jobs",
            "visible": true,
        },
        {
            "type": "link",
            "id": "nav-nodes-link",
            priority: 0,
            "container": "root",
            "group": "main",
            "class": "fas fa-sitemap",
            "link": "/project/test/nodes",
            "label": "Nodes",
            "visible": true,
        },
        {
            "type": "link",
            "id": "nav-commands-link",
            priority: 0,
            "container": "root",
            "group": "main",
            "class": "fas fa-terminal",
            "link": "/project/test/command/run",
            "label": "Commands",
            "visible": true,
        },
        {
            "type": "link",
            "id": "nav-activity-link",
            priority: 0,
            "container": "root",
            "group": "main",
            "class": "fas fa-history",
            "link": "/project/test/activity",
            "label": "Activity",
            "visible": true,
        },
        {
            "type": "link",
            "id": "nav-schedules-link",
            priority: 0,
            "container": "root",
            "group": "main",
            "class": "fas fa-clock",
            "link": "/project/test/schedules",
            "label": "Schedules",
            "visible": true,
        },
        {
            "type": "link",
            "id": "nav-healthcheck-link",
            priority: 0,
            "container": "root",
            "group": "main",
            "class": "fas fa-heartbeat",
            "link": "/project/test/healthcheck",
            "label": "Health Checks",
            "visible": true,
        },
        {
            "type": "link",
            "id": "nav-tours-link",
            priority: 0,
            "container": "root",
            "group": "main",
            "class": "glyphicon glyphicon-question-sign",
            "link": "/project/test/tourmanager",
            "label": "Tour Manager",
            "visible": true,
        },
        {
            "type": "link",
            "id": "nav-calendars-link",
            priority: 0,
            "container": "root",
            "group": "main",
            "class": "fas fa-calendar-alt",
            "link": "/project/test/calendars",
            "label": "Calendars",
            "visible": true,
        },
        {
            "type": "link",
            "id": "nav-reactions-link",
            priority: 0,
            "container": "root",
            "group": "main",
            "class": "fas fa-plug",
            "link": "/rdproreactions/project/test/reactions",
            "label": "Reactions",
            "visible": true,
        },
        {
            "type": "link",
            "id": "nav-webhooks-link",
            priority: 0,
            "container": "root",
            "group": "main",
            "class": "fas fa-plug",
            "link": "/webhook/admin?project=test",
            "label": "Webhooks",
            "visible": true,
        },
        {
            "type": "container",
            "id": "nav-project-settings",
            priority: 0,
            "container": "root",
            "group": "bottom",
            "class": "fas fa-cogs",
            "label": "Project Settings",
            "visible": true,
        }
    ] as Array<NavLink>)
    
    return Vue.extend({
        components: { NavBar },
        template: '<div id="section-navbar" style="width: 65px; height: 100%;overflow: hidden;"><NavBar /></div>',
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