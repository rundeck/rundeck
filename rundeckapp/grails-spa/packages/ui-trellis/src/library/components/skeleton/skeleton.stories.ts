import Vue from 'vue'

import { Rundeck, TokenCredentialProvider } from '@rundeck/client'
import {BrowserFetchHttpClient} from '@azure/ms-rest-js/es/lib/browserFetchHttpClient'

import '../../stories/setup'

import {RootStore} from '../../stores/RootStore'

import Skeleton from './Skeleton.vue'

export default {
    title: 'Skeleton'
}

export const list = () => {
    const rootStore = new RootStore(window._rundeck.rundeckClient)

    const loading =  true
    
    return Vue.extend({
        template: `<Skeleton v-bind="$props"><h1>Foo</h1><h1>Bar</h1></Skeleton>`,
        provide: {rootStore},
        components: {Skeleton},
        props: {
            loading: { default: loading}
        },
        mounted() {
            const el = this.$el as any
            el.parentNode.style.height = '100vh'
            el.parentNode.style.overflow = 'hidden'
            el.parentNode.style.position = 'relative'
            el.parentNode.style.padding = '20px'
            document.body.style.overflow = 'hidden'
        }
    })
}

export const listWithAvatar = () => {
    const rootStore = new RootStore(window._rundeck.rundeckClient)

    const loading = true
    
    return Vue.extend({
        template: `<Skeleton v-bind="$props"><h1>Foo</h1><h1>Bar</h1></Skeleton>`,
        provide: {rootStore},
        components: {Skeleton},
        props: {
            loading: { default: loading},
            type: { default: 'avatar-list'}
        },
        mounted() {
            const el = this.$el as any
            el.parentNode.style.height = '100vh'
            el.parentNode.style.overflow = 'hidden'
            el.parentNode.style.position = 'relative'
            el.parentNode.style.padding = '20px'
            document.body.style.overflow = 'hidden'
        }
    })
}