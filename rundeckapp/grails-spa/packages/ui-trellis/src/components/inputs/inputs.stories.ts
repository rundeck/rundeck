import Vue from 'vue'
import {addons} from '@storybook/addons'

import '../../stories/setup'

import InputSwitch from './Switch.vue'

export default {
    title: 'Inputs/Switch'
}

function setupStory(vue: Vue) {
    const el = vue.$el as any
    el.parentNode.style.height = '100vh'
    el.parentNode.style.overflow = 'hidden'
    el.parentNode.style.position = 'relative'
    el.parentNode.style.padding = '20px'
    document.body.style.overflow = 'hidden'
}

export const inputSwitch = () => {
    const chan = addons.getChannel()

    return Vue.extend({
        template: `<InputSwitch @input="handleChecked" :value="checked"/>`,
        components: {InputSwitch},
        props: {
            checked: {default:  false},
            disabled: {default:  false}
        },
        mounted() {
            setupStory(this)
        },
        methods: {
            handleChecked(val: boolean) {
                chan.emit('checked', val)
            }
        }
    })
}

export const inputSwitchOn = () => {
    return Vue.extend({
        template: `<InputSwitch v-bind="$props"/>`,
        components: {InputSwitch},
        props: {
            value: {default: true},
        },
        mounted() {
            setupStory(this)
        }
    })
}

export const inputSwitchDisabled = () => {
    return Vue.extend({
        template: `<div><InputSwitch :disabled="true"/><InputSwitch :disabled="true" :value="true"/></div>`,
        components: {InputSwitch},
        mounted() {
            setupStory(this)
        }
    })
}

export const inputSwitchContrast = () => {
    const chan = addons.getChannel()

    return Vue.extend({
        template: `
        <div style="background-color: var(--background-color-accent-lvl2);height: 100%;width: 100%">
        <InputSwitch @input="handleChecked" :value="checked" contrast/>
        </div>
        `,
        components: {InputSwitch},
        props: {
            checked: {default:  false},
            disabled: {default:  false}
        },
        mounted() {
            setupStory(this)
        },
        methods: {
            handleChecked(val: boolean) {
                chan.emit('checked', val)
            }
        }
    })
}