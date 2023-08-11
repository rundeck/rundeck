import type {Meta, StoryFn} from "@storybook/vue3"
import {addons} from '@storybook/addons'

import '../../../stories/setup'

import Tabs from './Tabs.vue'
import Tab from './Tab.vue'
import TabContent from './TabContent.vue'

export default {
    title: 'Containers/Tabs',
    component: Tab,
} as Meta<typeof Tab>

function setupStory(vue) {
    const el = vue.$el as any
    el.parentNode.style.height = '100vh'
    el.parentNode.style.overflow = 'hidden'
    el.parentNode.style.position = 'relative'
    el.parentNode.style.padding = '20px'
    document.body.style.overflow = 'hidden'
}

export const tabs: StoryFn<typeof Tab> = (args) => {
    const chan = addons.getChannel()

    return {
        setup() {
            return { args }
        },
        template: `
        <Tabs class="card" v-bind="args" style="padding: 20px;">
            <Tab :index="0" title="Tab"><TabContent>Foo Content</TabContent></Tab>
            <Tab :index="1" title="Tab Bar"><TabContent><input type="text"/></TabContent></Tab>
            <Tab :index="2" title="Tab Baz">Baz Content</Tab>
            <Tab :index="3" title="Tab Batch Processing">Batch Content</Tab>
        </Tabs>`,
        components: {Tabs, Tab, TabContent},
        mounted() {
            setupStory(this)
        },
        methods: {
            handleChecked(val: boolean) {
                chan.emit('checked',val)
            }
        }
    }
}
tabs.args = {
    type: 'standard'
}

export const cardTabs = (args) => {
    const chan = addons.getChannel()

    return {
        setup() {
            return { args }
        },
        template: `
        <div class="card" style="border-width: 0.1em;">
        <Tabs v-bind="args">
            <Tab :index="0" title="Tab"><TabContent>Foo Content</TabContent></Tab>
            <Tab :index="1" title="Tab Bar"><TabContent><input type="text"/></TabContent></Tab>
            <Tab :index="2" title="Tab Baz">Baz Content</Tab>
            <Tab :index="3" title="Tab Batch Processing">Batch Content</Tab>
        </Tabs>
        </div>`,
        components: {Tabs, Tab, TabContent},
        mounted() {
            setupStory(this)
        },
        methods: {
            handleChecked(val: boolean) {
                chan.emit('checked', val)
            }
        }
    }
}
cardTabs.args = {
    type: 'card',
}