import Vue from 'vue'
import {addons} from '@storybook/addons'
import {CHANGE, array ,object, boolean, withKnobs, select, text} from '@storybook/addon-knobs'

import '../../../stories/setup'

import Drawer from './Drawer.vue'

export default {
    title: 'Containers/Drawer',
    decorators: [withKnobs({disableDebounce: true})]
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
            visible: {default: boolean('Open', true)},
            placement: {default: select('Placement', {left: 'left', right: 'right', top: 'top'}, 'left')}
        },
        mounted() {
            setupStory(this)
        },
        methods: {
            close() {
                chan.emit(CHANGE, {name: 'Open', value: false})
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
            visible: {default: boolean('Open', true)},
            width: {default: text('Width', '50%')},
            height: {default: text('Height', '50%')},
            placement: {default: select('Placement', {left: 'left', right: 'right', top: 'top'}, 'left')}
        },
        mounted() {
            setupStory(this)
        },
        methods: {
            close() {
                chan.emit(CHANGE, {name: 'Open', value: false})
            }
        }
    })
}