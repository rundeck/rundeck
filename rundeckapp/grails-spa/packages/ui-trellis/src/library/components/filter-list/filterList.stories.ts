import type {Meta, StoryFn} from "@storybook/vue3";
import { Rundeck, TokenCredentialProvider } from '@rundeck/client'
import {BrowserFetchHttpClient} from '@azure/ms-rest-js/es/lib/browserFetchHttpClient'

import '../../stories/setup'

import {RootStore} from '../../stores/RootStore'

import FilterList from './FilterList.vue'

// @ts-ignore
window._rundeck.rundeckClient = new Rundeck(new TokenCredentialProvider(process.env.STORYBOOK_RUNDECK_TOKEN), {baseUri: process.env.STORYBOOK_RUNDECK_URL, httpClient: new BrowserFetchHttpClient()})


export default {
    title: 'Filter List',
    component: FilterList
} as Meta<typeof FilterList>


export const filterList: StoryFn<typeof FilterList> = () => {
    const rootStore = new RootStore(window._rundeck.rundeckClient)
    window._rundeck.rootStore = rootStore

    const items = [{label: 'Foo', name: 'Bar'}]

    setTimeout(() => {items.push({label: 'Fiz', name: 'Buz'})}, 2000)
    
    return {
        template: `
        <FilterList v-bind="$data">
            <template v-slot:item="item"><i class="fa fas fa-beer"/>Foo: {{item.name}}</template>
            <template v-slot:default"footer">
              <div>I'm a footer</div>
            </template>
        </FilterList>
        `,
        provide: {rootStore},
        components: {FilterList},
        data: () => ({
            items,
            searchPlaceholder: 'Search for plugins',
            loading: false
        }),
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



