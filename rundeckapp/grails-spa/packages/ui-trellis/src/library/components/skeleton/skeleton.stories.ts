import type {Meta, StoryFn} from '@storybook/vue3'

import { Rundeck, TokenCredentialProvider } from '@rundeck/client'
import {BrowserFetchHttpClient} from '@azure/ms-rest-js/es/lib/browserFetchHttpClient'

import '../../stories/setup'

import {RootStore} from '../../stores/RootStore'

import Skeleton from './Skeleton.vue'

export default {
    title: 'Skeleton',
    component: Skeleton
} as Meta<typeof Skeleton>

export const list: StoryFn<typeof Skeleton> = (args) => {
    const rootStore = new RootStore(window._rundeck.rundeckClient)

    window._rundeck.rootStore = rootStore
    return {
        setup() {
            return { args }
        },
        template: `<Skeleton v-bind="args"><h1>Foo</h1><h1>Bar</h1></Skeleton>`,
        provide: {rootStore},
        components: {Skeleton},
        mounted() {
            const el = this.$el as any
            el.parentNode.style.height = '100vh'
            el.parentNode.style.overflow = 'hidden'
            el.parentNode.style.position = 'relative'
            el.parentNode.style.padding = '20px'
            document.body.style.overflow = 'hidden'
        }
    }
}
list.args = {
    loading: true,
}

export const listWithAvatar: StoryFn<typeof Skeleton> = (args) => {
    const rootStore = new RootStore(window._rundeck.rundeckClient)

    window._rundeck.rootStore = rootStore

    return {
        setup() {
            return { args }
        },
        template: `<Skeleton v-bind="args"><h1>Foo</h1><h1>Bar</h1></Skeleton>`,
        provide: {rootStore},
        components: {Skeleton},
        mounted() {
            const el = this.$el as any
            el.parentNode.style.height = '100vh'
            el.parentNode.style.overflow = 'hidden'
            el.parentNode.style.position = 'relative'
            el.parentNode.style.padding = '20px'
            document.body.style.overflow = 'hidden'
        }
    }
}
listWithAvatar.args = {
    loading: true,
    type: 'avatar-list',
}