import Vue from 'vue'
import {array ,object, withKnobs} from '@storybook/addon-knobs'

import { Rundeck, RundeckClient, TokenCredentialProvider } from '@rundeck/client'
import {BrowserFetchHttpClient} from '@azure/ms-rest-js/es/lib/browserFetchHttpClient'

import '../../../stories/setup'

import SystemConfigSelect from './SystemConfigSelect.vue'

// @ts-ignore



export default {
    title: 'Widgets/System Config Select',
    decorators: [withKnobs]
}


export const pluginPicker = () => {    

    return Vue.extend({
        template: `<SystemConfigSelect :categories="categories" @item:selected="() => {}"/>`,

        components: {SystemConfigSelect},
        data: () => ({
            categories: [
                {name: "Cluster", isSubcat: false, parentCategory: null},
                {name: "Plugins", isSubcat: false, parentCategory: null},
                {name: "Azure", isSubcat: true, parentCategory: "Plugins"},
                {name: "Datadoge", isSubcat: true, parentCategory: "Plugins"}
            ],
            selected: ''
        }),
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
