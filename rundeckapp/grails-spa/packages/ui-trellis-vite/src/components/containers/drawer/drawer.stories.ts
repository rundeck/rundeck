import Vue from 'vue'
import {addons} from '@storybook/addons'

import '../../../stories/setup'

import Drawer from './Drawer.vue'

export default {
    title: 'Containers/Drawer'
}

function setupStory(vue: Vue) {
    const el = vue.$el as any
    el.parentNode.style.height = '100vh'
    el.parentNode.style.overflow = 'hidden'
    el.parentNode.style.position = 'relative'
    el.parentNode.style.padding = '20px'
    document.body.style.overflow = 'hidden'
}

export const drawer = () => {
    const chan = addons.getChannel()

    return Vue.extend({
        template: `
        <div style="height: 100%;width: 100%;background-color: beige;position: relative;overflow: hidden;">
            <Drawer v-bind="$props" @close="close">Foo</Drawer>
        </div>`,
        components: {Drawer},
        props: {
            title: {default: 'Settings'},
            visible: {default:  true},
            placement: {default: 'left'}
        },
        mounted() {
            setupStory(this)
        },
        methods: {
            close() {
                chan.emit('Open', false)
            }
        }
    })
}

export const autoSize = () => {
    const chan = addons.getChannel()

    return Vue.extend({
        template: `
        <div style="height: 100%;width: 100%;background-color: beige;position: relative;overflow: hidden;">
            <Drawer v-bind="$props" @close="close">Foo</Drawer>
        </div>`,
        components: {Drawer},
        props: {
            title: {default: 'Settings'},
            visible: {default:  true},
            width: {default: '50%'},
            height: {default:  '50%'},
            placement: {default:  'left'}
        },
        mounted() {
            setupStory(this)
        },
        methods: {
            close() {
                chan.emit( 'Open', false)
            }
        }
    })
}