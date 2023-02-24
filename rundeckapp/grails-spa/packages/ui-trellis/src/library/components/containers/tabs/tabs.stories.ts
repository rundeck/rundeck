import Vue from 'vue'
import {addons} from '@storybook/addons'

import '../../../stories/setup'

import Tabs from './Tabs'
import Tab from './Tab'
import TabContent from './TabContent.vue'

export default {
    title: 'Containers/Tabs'
}

function setupStory(vue: Vue) {
    const el = vue.$el as any
    el.parentNode.style.height = '100vh'
    el.parentNode.style.overflow = 'hidden'
    el.parentNode.style.position = 'relative'
    el.parentNode.style.padding = '20px'
    document.body.style.overflow = 'hidden'
}

export const tabs = () => {
    const chan = addons.getChannel()

    return Vue.extend({
        template: `
        <Tabs class="card" v-bind="$props" style="padding: 20px;">
            <Tab :index="0" title="Tab"><TabContent>Foo Content</TabContent></Tab>
            <Tab :index="1" title="Tab Bar"><TabContent><input type="text"/></TabContent></Tab>
            <Tab :index="2" title="Tab Baz">Baz Content</Tab>
            <Tab :index="3" title="Tab Batch Processing">Batch Content</Tab>
        </Tabs>`,
        components: {Tabs, Tab, TabContent},
        props: {
            type: {default: 'standard'}
        },
        mounted() {
            setupStory(this)
        },
        methods: {
            handleChecked(val: boolean) {
                chan.emit('checked',val)
            }
        }
    })
}

export const cardTabs = () => {
    const chan = addons.getChannel()

    return Vue.extend({
        template: `
        <div class="card" style="border-width: 0.1em;">
        <Tabs v-bind="$props">
            <Tab :index="0" title="Tab"><TabContent>Foo Content</TabContent></Tab>
            <Tab :index="1" title="Tab Bar"><TabContent><input type="text"/></TabContent></Tab>
            <Tab :index="2" title="Tab Baz">Baz Content</Tab>
            <Tab :index="3" title="Tab Batch Processing">Batch Content</Tab>
        </Tabs>
        </div>`,
        components: {Tabs, Tab, TabContent},
        props: {
            type: {default:  'card'}
        },
        mounted() {
            setupStory(this)
        },
        methods: {
            handleChecked(val: boolean) {
                chan.emit('checked', val)
            }
        }
    })
}