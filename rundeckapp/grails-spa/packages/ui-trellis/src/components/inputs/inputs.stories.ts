import Vue from 'vue'
import {addons} from '@storybook/addons'
import {CHANGE, array ,object, boolean, withKnobs} from '@storybook/addon-knobs'

import '../../stories/setup'

import InputSwitch from './Switch.vue'

export default {
    title: 'Inputs/Switch',
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

export const inputSwitch = () => {
    const chan = addons.getChannel()

    return Vue.extend({
        template: `<InputSwitch @input="handleChecked" :value="checked"/>`,
        components: {InputSwitch},
        props: {
            checked: {default: boolean('checked', false)},
            disabled: {default: boolean('disabled', false)}
        },
        mounted() {
            setupStory(this)
        },
        methods: {
            handleChecked(val: boolean) {
                chan.emit(CHANGE, {name: 'checked', value: val})
            }
        }
    })
}

export const inputSwitchEnabled = () => {
    return Vue.extend({
        template: `<InputSwitch v-bind="$props"/>`,
        components: {InputSwitch},
        props: {
            checked: {default: boolean('checked', true)},
            disabled: {default: boolean('disabled', false)}
        },
        mounted() {
            setupStory(this)
        }
    })
}

export const inputSwitchDisabled = () => {
    return Vue.extend({
        template: `<InputSwitch v-bind="$props"/>`,
        components: {InputSwitch},
        props: {
            checked: {default: boolean('checked', true)},
            disabled: {default: boolean('disabled', true)}
        },
        mounted() {
            setupStory(this)
        }
    })
}