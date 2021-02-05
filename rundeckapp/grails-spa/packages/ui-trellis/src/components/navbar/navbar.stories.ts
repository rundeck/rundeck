import Vue from 'vue'
import {array ,object, withKnobs} from '@storybook/addon-knobs'

import NavBar from './NavBar.vue'

import {NavItem} from '../../stores/NavBar'
import {RootStore} from '../../stores/RootStore'

export default {
    title: 'Navigation Bar',
    decorators: [withKnobs]
}

export const navBar = () => {
    const rootStore = new RootStore(window._rundeck.rundeckClient)

    rootStore.navBar.items.push({"id": "foo"})
    
    return Vue.extend({
        components: { NavBar },
        template: '<NavBar />',
        mounted: function() {
            const el = this.$el as any
            el.parentNode.style.height = '100vh'
        },
        provide: () => ({
            rootStore: rootStore,
        })
    })
}