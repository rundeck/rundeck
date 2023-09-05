import type {Meta, StoryFn} from '@storybook/vue3'

import ServerDisplay from './ServerDisplay.vue'

export default {
    title: 'Rundeck Version Atoms/ServerDisplay',
    component: ServerDisplay,
} as Meta<typeof ServerDisplay>

export const serverInfo: StoryFn<typeof ServerDisplay> = () => ({
    components: { ServerDisplay },
    data: () => ({
        name: 'xubuntu',
        glyphicon: 'flash',
        uuid: 'f1dbb7ed-c575-4154-8d01-216a59d7cb5e'
    }),
    template: '<ServerDisplay v-bind="$data"/>',
});
