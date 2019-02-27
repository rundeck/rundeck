<template>
    <div id="project-nodes-config">
      <span class="h4">
        Nodes Plugins
        </span>
        <div class="list-group">
            <div class="list-group-item" v-for="(plugin,index) in plugins" :key="'plugin_'+index">
                {{index+1}}.
                <plugin-info :detail="plugin.detail" v-if="plugin.detail"></plugin-info>
                <span v-else>
                {{plugin.type}}
                </span>
                 <plugin-config :mode="'show'"
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

<script>
import axios from 'axios'
import Vue from 'vue'
import Trellis, {
    getRundeckContext,
    getSynchronizerToken,
    RundeckBrowser
} from "@rundeck/ui-trellis"

import PluginInfo from '@/components/plugins/PluginInfo'
import PluginConfig from '@/components/plugins/pluginConfig'

export default {
    name: 'App',
    components: {
        PluginInfo,
        PluginConfig
    },
    data () {
        return {
            project: null,
            rdBase: null,
            plugins: [],
            RundeckContext: null,
            ServiceProvidersCache: {}
        }
    },
    methods: {
        getPluginDescription (svcName, provider) {
            if (this.ServiceProvidersCache[svcName] && this.ServiceProvidersCache[svcName].providers[provider]) {
                return new Promise((resolve, reject) => {
                    resolve(this.ServiceProvidersCache[svcName].providers[provider])
                })
            }

            return this.RundeckContext.rundeckClient.sendRequest({
                url: `/plugin/detail/${svcName}/${provider}`,
                method: 'GET'
            }).then(resp => {
                if (!resp.parsedBody) {
                    throw new Error(`Error getting service provider detail for ${svcName}/${provider}`)
                }
                if (!this.ServiceProvidersCache[svcName]) {
                    this.ServiceProvidersCache[svcName] = {
                        providers: {}
                    }
                }
                this.ServiceProvidersCache[svcName].providers[provider] = resp.parsedBody
                return resp.parsedBody
            }).catch(e => console.warn('Error getting service provider detail', e))
        }
    },
    mounted () {
        this.RundeckContext = getRundeckContext()
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
                    this.plugins = response.data.nodeEnhancerPlugins
                    let self = this
                    console.log("got pugins", this.plugins)
                    this.plugins.forEach(function (value,x) {
                        self.getPluginDescription('NodeEnhancer', value.type).then(function (pluginDetail) {
                            Vue.set(value, 'detail', pluginDetail)
                        })
                    })
                }
            })
        }
    }
}
</script>

<style>
</style>
