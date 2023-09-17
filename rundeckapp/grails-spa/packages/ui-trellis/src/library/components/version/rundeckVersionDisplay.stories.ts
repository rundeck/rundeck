import type {Meta, StoryFn} from '@storybook/vue3'

import RundeckVersionDisplay from './RundeckVersionDisplay.vue'

export default {
    title: 'Rundeck Version Atoms/RundeckVersionDisplay',
    component: RundeckVersionDisplay,
} as Meta<typeof RundeckVersionDisplay>

export const rundeckVersion: StoryFn<typeof RundeckVersionDisplay> = () => ({
    components: { RundeckVersionDisplay },
    data: () => ({
        edition: 'Community',
        number: '3.4.0',
        tag: 'SNAPSHOT'
    }),
    template: `<RundeckVersionDisplay v-bind="$data"/>`
})
