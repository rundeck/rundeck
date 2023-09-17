import type {Meta, StoryFn} from '@storybook/vue3'
import {addons} from '@storybook/addons'

import '../../../stories/setup'

import Drawer from './Drawer.vue'

export default {
    title: 'Containers/Drawer',
    component: Drawer,
} as Meta<typeof Drawer>

function setupStory(vue) {
    const el = vue.$el as any
    el.parentNode.style.height = '100vh'
    el.parentNode.style.overflow = 'hidden'
    el.parentNode.style.position = 'relative'
    el.parentNode.style.padding = '20px'
    document.body.style.overflow = 'hidden'
}

export const drawer: StoryFn<typeof Drawer> = (args) => {
    const chan = addons.getChannel()

    return {
        setup() {
           return { args }
        },
        template: `
        <div style="height: 100%;width: 100%;background-color: beige;position: relative;overflow: hidden;">
            <Drawer v-bind="args" @close="close">Foo</Drawer>
        </div>`,
        components: {Drawer},
        mounted() {
            setupStory(this)
        },
        methods: {
            close() {
                chan.emit('Open', false)
            }
        }
    }
}
drawer.args = {
    title: 'Settings',
    visible: true,
    placement: 'left',
}

export const autoSize: StoryFn<typeof Drawer> = (args) => {
    const chan = addons.getChannel()

    return {
        setup() {
            return {args}
        },
        template: `
        <div style="height: 100%;width: 100%;background-color: beige;position: relative;overflow: hidden;">
            <Drawer v-bind="args" @close="close">Foo</Drawer>
        </div>`,
        components: {Drawer},
        mounted() {
            setupStory(this)
        },
        methods: {
            close() {
                chan.emit( 'Open', false)
            }
        }
    }
}
autoSize.args = {
    title: 'Settings',
    visible:  true,
    width: '50%',
    height:  '50%',
    placement:  'left'
}