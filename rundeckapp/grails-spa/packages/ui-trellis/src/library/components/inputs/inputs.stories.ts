import type {Meta, StoryFn} from "@storybook/vue3"
import {addons} from '@storybook/addons'

import '../../stories/setup'

import InputSwitch from './Switch.vue'

export default {
    title: 'Inputs/Switch',
    component: InputSwitch
} as Meta<typeof InputSwitch>

function setupStory(vue) {
    const el = vue.$el as any
    el.parentNode.style.height = '100vh'
    el.parentNode.style.overflow = 'hidden'
    el.parentNode.style.position = 'relative'
    el.parentNode.style.padding = '20px'
    document.body.style.overflow = 'hidden'
}

export const inputSwitch: StoryFn<typeof InputSwitch> = (args) => {
    const chan = addons.getChannel()

    return {
        setup() {
            return { ...args }
        },
        template: `<InputSwitch @input="handleChecked" :value="checked"/>`,
        components: {InputSwitch},
        mounted() {
            setupStory(this)
        },
        methods: {
            handleChecked(val: boolean) {
                chan.emit('checked', val)
            }
        }
    }
}
inputSwitch.args = {
    checked: false,
}

export const inputSwitchOn: StoryFn<typeof InputSwitch> = (args) => {
    return {
        setup() {
            return { args }
        },
        template: `<InputSwitch v-bind="args"/>`,
        components: {InputSwitch},
        mounted() {
            setupStory(this)
        }
    }
}
inputSwitchOn.args = {
    value: true,
}

export const inputSwitchDisabled: StoryFn<typeof InputSwitch> = () => {
    return {
        template: `<div><InputSwitch :disabled="true"/><InputSwitch :disabled="true" :value="true"/></div>`,
        components: {InputSwitch},
        mounted() {
            setupStory(this)
        }
    }
}

export const inputSwitchContrast: StoryFn<typeof InputSwitch> = (args) => {
    const chan = addons.getChannel()

    return {
        setup() {
            return { ...args }
        },
        template: `
        <div style="background-color: var(--background-color-accent-lvl2);height: 100%;width: 100%">
        <InputSwitch @input="handleChecked" :value="checked" contrast/>
        </div>
        `,
        components: {InputSwitch},
        mounted() {
            setupStory(this)
        },
        methods: {
            handleChecked(val: boolean) {
                chan.emit('checked', val)
            }
        }
    }
}
inputSwitchContrast.args = {
    checked: false,
}