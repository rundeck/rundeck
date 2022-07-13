import Vue from 'vue'
import {addons} from '@storybook/addons'

import CopyBox from './CopyBox.vue'

export default {
    title: 'Containers'
}

function setupStory(vue: Vue) {
    const el = vue.$el as any
    el.parentNode.style.height = '100vh'
    el.parentNode.style.overflow = 'hidden'
    el.parentNode.style.position = 'relative'
    el.parentNode.style.padding = '20px'
    document.body.style.overflow = 'hidden'
}

export const copyBox = () => {

    return Vue.extend({
        template: `
        <CopyBox v-bind="$props"/>`,
        components: {CopyBox},
        props: {
            content: {default: 'Text to copy!'}
        },
        mounted() {
            setupStory(this)
        },
        methods: {
        }
    })
}


export const copyBoxLongContent = () => {

    return Vue.extend({
        template: `
        <CopyBox v-bind="$props"/>`,
        components: {CopyBox},
        props: {
            content: {default: 'http://localhost:8080/api/40/webhook/OSUK4zGjC9jx1Dhfdi9SYaaADUTBBhuZ#Generic'}
        },
        mounted() {
            setupStory(this)
        },
        methods: {
        }
    })
}