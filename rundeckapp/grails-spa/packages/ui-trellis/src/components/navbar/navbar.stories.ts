import Vue from 'vue'
import {object, withKnobs} from '@storybook/addon-knobs'

import NavBar from './NavBar.vue'

import {RootStore} from '../../stores/RootStore'

export default {
    title: 'Navigation Bar',
    decorators: [withKnobs]
}

export const navBar = () => (Vue.extend({
    components: { NavBar },
    template: '<NavBar />',
    mounted: function() {
        const el = this.$el as any
        el.parentNode.style.height = '100vh'
    },
    provide: () => ({
        rootStore: new RootStore(window._rundeck.rundeckClient),
    })
}))