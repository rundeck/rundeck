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
        template: `<SystemConfigSelect :categories="currentCategories" @item:selected="() => {}"/>`,

        components: {SystemConfigSelect},
        data: () => ({
            currentCategories: {"Plugins":
            [{
                    "key": "azure.tenantId",
                    "visibility": "Standard",
                    "category": "Plugins",
                    "strata": "default",
                    "required": false,
                    "restart": false,
                    "label": "Azure Tenant ID",
                    "datatype": "String",
                    "encrypted": false,
                    "defaultValue": ""
                  },
                  {
                    "key": "datadog.api-key-storage-path",
                    "visibility": "Standard",
                    "category": "Plugins",
                    "strata": "default",
                    "required": false,
                    "restart": false,
                    "label": "DataDog API Key Path",
                    "datatype": "storage-key",
                    "encrypted": false,
                    "defaultValue": ""
                  }],
                  "Cluster": [
                    {
                        "key": "rundeck.clusterMode.remoteExecution.policy",
                        "visibility": "Standard",
                        "category": "Cluster",
                        "strata": "default",
                        "required": false,
                        "restart": true,
                        "label": "Cluster Remote Exection - Policy",
                        "datatype": "List",
                        "encrypted": false,
                        "defaultValue": "None"
                      },
                  ]
            },
            idealNewCategories: [
                {name: "Cluster", isSubcat: false, parentCategory: null},
                {name: "Plugins", isSubcat: false, parentCategory: null},
                {name: "Azure", isSubcat: true, parentCategory: "Plugins", aliases: ["Plugins"]},
                {name: "Datadoge", isSubcat: true, parentCategory: "Plugins", aliases: ["Plugins"]}
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
        },
    })
}
