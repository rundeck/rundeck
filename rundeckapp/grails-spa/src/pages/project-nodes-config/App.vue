<template>
  <div class="card">
    <div class="card-header">
        <h3 class="card-title">
            {{title}}
        </h3>
        <btn type="info" @click="mode='edit'" v-if="mode!=='edit'">{{$t('edit')}}</btn>
    </div>

    <div class="card-content">

      <div id="project-nodes-config">

          <div class="list-group">
              <div class="list-group-item" v-for="(plugin,index) in pluginConfigs" :key="'plugin_'+index">
                  <plugin-config
                    :mode="'title'"
                    :serviceName="serviceName"
                    :provider="plugin.type"
                    :key="plugin.type+'title/'+index"
                    >
                    <span slot="titlePrefix">{{index+1}}.</span>
                  </plugin-config>
                  <plugin-config
                    :mode="editFocus===(index) ? plugin._new?'create':'edit':'show'"
                    :serviceName="serviceName"
                    v-model="pluginConfigs[index]"
                    :provider="plugin.type"
                    :config="plugin.config"
                    :key="plugin.type+'_config/'+index"
                    :show-title="false"
                    :show-description="false"
                    :validation="pluginValidation[index]"
                    >
                     <div slot="extra" class="row" v-if="mode==='edit'">
                            <div class="col-xs-12 col-sm-12">
                              <span v-if="editFocus===-1" >
                                  <btn class="btn-primary btn-xs" @click="editFocus=index">{{$t('edit')}}</btn>
                              </span>
                              <span v-if="editFocus===index" >
                                  <btn  class="btn-success btn-xs" @click="savePlugin(plugin,index+'')">{{$t('save')}}</btn>
                              </span>
                              <div class="btn-group pull-right" v-if="(editFocus===-1||editFocus===index)">
                                <btn class="btn-xs btn-danger" @click="removePlugin(plugin,index+'')" :disabled="editFocus!==-1&&editFocus!==(index)">{{$t('delete')}} <i class="fas fa-minus"></i></btn>
                                <btn class="btn-xs" @click="movePlugin(index,plugin,-1)" :disabled="editFocus!==-1 || index==0"><i class="fas fa-arrow-up"></i></btn>
                                <btn class="btn-xs" @click="movePlugin(index,plugin,1)" :disabled="editFocus!==-1 || index>=pluginConfigs.length-1"><i class="fas fa-arrow-down"></i></btn>
                              </div>
                            </div>
                          </div>
                    </plugin-config>

              </div>


              <div class="btn-group "  v-if="mode==='edit' && editFocus===-1">

                <dropdown ref="dropdown">
                  <btn type="primary" class="dropdown-toggle">
                    {{pluginLabels&&pluginLabels.addButton||serviceName}}
                    <span class="caret"></span>
                  </btn>
                  <template slot="dropdown">

                    <li v-for="plugin in pluginProviders" v-bind:key="plugin.name">
                        <a href="#" @click="addPlugin(plugin.name)">
                          <plugin-info :detail="plugin"/>
                        </a>
                    </li>
                  </template>
                </dropdown>
          </div>
          <div v-if="mode==='edit' && editFocus===-1">
            <btn type="default" @click="cancelAction">{{$t('cancel')}}</btn>
            <btn type="success" @click="savePlugins">{{$t('save')}}</btn>
          </div>
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
import pluginService from '@rundeck/ui-trellis/src/modules/pluginService'
import PluginValidation from '@rundeck/ui-trellis/src/interfaces/PluginValidation'
import {client} from '@rundeck/ui-trellis/src/modules/rundeckClient'

export default Vue.extend({
    name: 'App',
    components: {
        PluginInfo,
        PluginConfig,
        Expandable
    },
    data () {
        return {
            title:'Project Plugins',
            mode: 'show',
            project: '',
            rdBase: '',
            configPrefix:'',
            serviceName:'',
            cancelUrl:'',
            pluginConfigs: [] as any[],
            configOrig:[] as any[],
            rundeckContext: {} as RundeckContext,
            modalAddOpen:false,
            pluginProviders:[],
            pluginLabels:{},
            editFocus:-1,
            pluginValidation:{} as  {[index:string]:PluginValidation}
        }
    },
    methods: {
      addPlugin(provider:string){
          this.pluginConfigs.push({type:provider,config:{},_new:true})
          this.editFocus=this.pluginConfigs.length-1
      },
      async savePlugin(plugin:any,index:string){
        //validate

        const validation:PluginValidation = await pluginService.validatePluginConfig(this.serviceName, plugin.type, plugin.config)
        if(!validation.valid){
          Vue.set(this.pluginValidation,index,validation)
          return
        }
        Vue.delete(this.pluginValidation,index)
        this.editFocus = -1
      },
      removePlugin(plugin:any,index:string){
        const found=this.pluginConfigs.indexOf(plugin)
        this.pluginConfigs.splice(found,1)
        Vue.delete(this.pluginValidation,index)
        this.editFocus=-1
      },

      movePlugin(index:number,plugin:any,shift:number){
        const found=this.pluginConfigs.indexOf(plugin)
        const item= this.pluginConfigs.splice(index,1)[0]
        const newindex=found+shift
        this.pluginConfigs.splice(newindex,0,item)
        const origValidation = this.pluginValidation[newindex+'']
        Vue.set(this.pluginValidation,newindex+'',this.pluginValidation[index+''])
        Vue.set(this.pluginValidation,index+'',origValidation)
        this.editFocus=-1
      },
      async savePlugins () {
        const result= await this.saveProjectPluginConfig(this.project, this.configPrefix, this.serviceName, this.pluginConfigs)
        this.mode='show'
      },
      async loadProjectPluginConfig(project:string,configPrefix:string,serviceName:string){
        const response = await axios({
            method: 'get',
            headers: {'x-rundeck-ajax': true},
            url: `${this.rdBase}framework/projectPluginsAjax`,
            params: {
                project: `${window._rundeck.projectName}`,
                configPrefix: this.configPrefix,
                serviceName: this.serviceName,
                format: 'json'
            },
            withCredentials: true
        })
        if (response.data && response.data.plugins) {
            return response.data.plugins
        }
      },
      async saveProjectPluginConfig(project:string,configPrefix:string,serviceName:string,data:any){
        const resp = await client.sendRequest({
          url: `/framework/saveProjectPluginsAjax`,
          method: 'POST',
          queryParameters: {
                project: `${window._rundeck.projectName}`,
                configPrefix: this.configPrefix,
                serviceName: this.serviceName,
                },
          body: {plugins:data}
        })
        if (!resp.parsedBody) {
          throw new Error(`Error saving config ${serviceName}`)
        } else {
          return resp.parsedBody
        }
      },
      cancelAction(){
        this.pluginConfigs= this.configOrig.map((obj:any)=>{
          return Object.assign({},obj)
        })
        this.mode='show'
      }
    },
    mounted () {
        this.rundeckContext = getRundeckContext()
        const self=this
        if (window._rundeck && window._rundeck.rdBase && window._rundeck.projectName) {
            this.rdBase = window._rundeck.rdBase
            this.project = window._rundeck.projectName
            this.serviceName=window._rundeck.data['serviceName']||''
            this.configPrefix=window._rundeck.data['configPrefix']||''
            this.title=window._rundeck.data['title']||this.title

            this.loadProjectPluginConfig(window._rundeck.projectName,this.configPrefix,this.serviceName).then((pluginConfigs)=>{
              self.configOrig =  pluginConfigs
              //copy
              self.pluginConfigs = pluginConfigs.map((obj:any)=>{
                return Object.assign({},obj)
              })
            })
            pluginService.getPluginProvidersForService(this.serviceName).then(data=>{
                if (data.service) {
                  this.pluginProviders = data.descriptions
                  this.pluginLabels = data.labels
                }
            })
        }
    }
})
</script>

<style>
</style>
