import Vue from 'vue'
import {addons} from '@storybook/addons'
import {CHANGE, array ,object, boolean, withKnobs} from '@storybook/addon-knobs'

import VersionDisplay from './VersionDisplay.vue'

export default {
    title: 'Version/VersionDisplay',
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


export const versionDisplay = () => {
    return Vue.extend({
        template: `<VersionDisplay v-bind="$props"/>`,
        components: { VersionDisplay},
        props: {
            version: {
                default: '3.3.3'
            },
            date: {
                default: '11/10/2021'
            }
        },
        mounted() {
            setupStory(this)
        }
    })
}
