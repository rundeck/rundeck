<template>
    <div id="project-nodes-config">
        <span class="h4">
          Nodes Plugins
        </span>

        <div class="list-group">
            <div class="list-group-item" v-for="(plugin,index) in nodesPlugins" :key="'plugin_'+index">

                 <plugin-config
                  :mode="'title'"
                  :serviceName="'NodeEnhancer'"
                  :provider="plugin.type"
                  >
                  <span slot="titlePrefix">{{index+1}}.</span>
                  </plugin-config>
                <plugin-config
                  :mode="'show'"
                  :serviceName="'NodeEnhancer'"
                  :provider="plugin.type"
                      :config="plugin.config"
                      :show-title="false"
                      :show-description="false"
                  ></plugin-config>

            </div>
        </div>
    </div>
</template>

<script lang="ts">
import axios from 'axios'
import Vue from 'vue'
import Trellis, {
    getRundeckContext,
    getSynchronizerToken,
    RundeckBrowser,
    RundeckContext
} from "@rundeck/ui-trellis"

import Expandable from '@rundeck/ui-trellis/src/components/utils/Expandable.vue'
import PluginInfo from '@rundeck/ui-trellis/src/components/plugins/PluginInfo.vue'
import PluginConfig from '@rundeck/ui-trellis/src/components/plugins/pluginConfig.vue'

export default Vue.extend({
    name: 'App',
    components: {
        PluginInfo,
        PluginConfig,
        Expandable
    },
    data () {
        return {
            project: null as string|null,
            rdBase: null as string|null,
            nodesPlugins: [],
            rundeckContext: {} as RundeckContext,
            ServiceProvidersCache: {} as {[name:string]:{[provider:string]:{[detail:string]:any}}}
        }
    },
    methods: {

    },
    mounted () {
        this.rundeckContext = getRundeckContext()
        const self=this
        if (window._rundeck && window._rundeck.rdBase && window._rundeck.projectName) {
            this.rdBase = window._rundeck.rdBase
            this.project = window._rundeck.projectName
            axios({
                method: 'get',
                headers: {'x-rundeck-ajax': true},
                url: `${this.rdBase}framework/projectNodePluginsAjax`,
                params: {
                    project: `${window._rundeck.projectName}`,
                    format: 'json'
                },
                withCredentials: true
            }).then((response) => {
                if (response.data && response.data.nodeEnhancerPlugins) {
                    self.nodesPlugins = response.data.nodeEnhancerPlugins

                }
            })
        }
    }
})
</script>

<style>
</style>
