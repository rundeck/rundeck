import type {Meta, StoryFn} from "@storybook/vue3";

import CopyBox from './CopyBox.vue'

export default {
    title: 'Containers',
    component: CopyBox,
} as Meta<typeof CopyBox>

function setupStory(vue) {
    const el = vue.$el as any
    el.parentNode.style.height = '100vh'
    el.parentNode.style.overflow = 'hidden'
    el.parentNode.style.position = 'relative'
    el.parentNode.style.padding = '20px'
    document.body.style.overflow = 'hidden'
}

export const copyBox: StoryFn<typeof CopyBox> = (args) => {

    return {
        setup() {
            return { args }
        },
        template: `
        <CopyBox v-bind="args"/>`,
        components: {CopyBox},
        mounted() {
            setupStory(this)
        },
        methods: {
        }
    }
}
copyBox.args = {
    content: 'Text to copy!'
}


export const copyBoxLongContent: StoryFn<typeof CopyBox> = (args) => {

    return {
        setup() {
            return { args }
        },
        template: `
        <CopyBox v-bind="args"/>`,
        components: {CopyBox},
        mounted() {
            setupStory(this)
        },
        methods: {
        }
    }
}
copyBoxLongContent.args = {
    content: 'http://localhost:8080/api/40/webhook/OSUK4zGjC9jx1Dhfdi9SYaaADUTBBhuZ#Generic'
}