import type {Meta, StoryFn} from "@storybook/vue3"
import {Rundeck, TokenCredentialProvider} from "@rundeck/client";
import {BrowserFetchHttpClient} from "@azure/ms-rest-js/es/lib/browserFetchHttpClient";

import {RootStore} from "../../../stores/RootStore";
import ProjectSelectButton from './ProjectSelectButton.vue'


export default {
    title: 'Widgets/Project Select Button',
    component: ProjectSelectButton
} as Meta<typeof ProjectSelectButton>

// @ts-ignore
window._rundeck.rundeckClient = new Rundeck(new TokenCredentialProvider(process.env.STORYBOOK_RUNDECK_TOKEN), {baseUri: process.env.STORYBOOK_RUNDECK_URL, httpClient: new BrowserFetchHttpClient()})

export const pickerButton: StoryFn = () => {
    const rootStore = new RootStore(window._rundeck.rundeckClient)

    return {
        template: `<ProjectSelectButton v-bind="$data"/>`,
        provide: {rootStore},
        components: {ProjectSelectButton},
        data: () => ({
            projectLabel: 'Test'
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

export const pickerButtonNoLabel = () => {
    const rootStore = new RootStore(window._rundeck.rundeckClient)

    return {
        template: `<ProjectSelectButton v-bind="$data"/>`,
        provide: {rootStore},
        components: {ProjectSelectButton},
        data: () => ({
            projectLabel: ''
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