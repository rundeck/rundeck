<template>
  <div class="card">
    <div class="card-header">
        <h3 class="card-title">
            {{title}}
          <btn type="info" @click="mode='edit'" v-if="mode!=='edit'" >{{$t('edit')}}</btn>
        </h3>
    </div>

    <div class="card-content">
      <div v-if="errors.length>0" class="alert alert-danger">
          <ul>
            <li v-for="(error,index) in errors" :key="'error_'+index">
              {{error}}
            </li>
          </ul>
      </div>
      <div id="project-plugin-config">

          <div class="list-group">
              <div class="list-group-item" v-for="(plugin,index) in pluginConfigs" :key="'plugin_'+index">
                  <plugin-config
                    :mode="'title'"
                    :serviceName="serviceName"
                    :provider="plugin.entry.type"
                    :key="plugin.entry.type+'title/'+index"
                    >
                    <span slot="titlePrefix">{{index+1}}.</span>
                  </plugin-config>
                   <div  v-if="additionalProps && additionalProps.props.length>0">

                      <plugin-config :mode="editFocus===(index) ? plugin.create?'create':'edit':'show'"
                                          v-model="plugin.extra"
                                          :config="plugin.extra.config"
                                          :key="'additional_config_'+index+'/'+((editFocus===index)?'true':'false' )"
                                          :show-title="false"
                                          :show-description="false"
                                          :plugin-config="additionalProps"

                            ></plugin-config>
                  </div>
                  <plugin-config
                    :mode="editFocus===(index) ? plugin.create?'create':'edit':'show'"
                    :serviceName="serviceName"
                    v-model="plugin.entry"
                    :provider="plugin.entry.type"
                    :config="plugin.entry.config"
                    :key="plugin.entry.type+'_config/'+index"
                    :show-title="false"
                    :show-description="false"
                    :validation="plugin.validation"
                    >
                     <div slot="extra" class="row" v-if="mode==='edit'">

                            <div class="col-xs-12 col-sm-12">
                              <span v-if="editFocus===-1" >
                                  <btn class="btn-primary btn-xs" @click="editFocus=index" :key="edit">{{$t('edit')}}</btn>
                              </span>
                              <span v-if="editFocus===index" >
                                  <btn  class="btn-success btn-xs" @click="savePlugin(plugin,index)" :key="save">{{$t('save')}}</btn>
                              </span>
                              <div class="btn-group pull-right" v-if="(editFocus===-1||editFocus===index)">
                                <btn class="btn-xs btn-danger" @click="removePlugin(plugin,index)" :disabled="editFocus!==-1&&editFocus!==(index)">{{$t('delete')}} <i class="fas fa-minus"></i></btn>
                                <btn class="btn-xs" @click="movePlugin(index,plugin,-1)" :disabled="editFocus!==-1 || index==0"><i class="fas fa-arrow-up"></i></btn>
                                <btn class="btn-xs" @click="movePlugin(index,plugin,1)" :disabled="editFocus!==-1 || index>=pluginConfigs.length-1"><i class="fas fa-arrow-down"></i></btn>
                              </div>
                            </div>
                          </div>
                    </plugin-config>

              </div>

              <div class="list-group-item"  v-if="mode==='edit' && editFocus===-1">
              <div class="btn-group " >

                <dropdown ref="dropdown">
                  <btn type="primary" class="dropdown-toggle">
                    <i class="fas fa-plus"></i>
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
            </div>
      </div>
      <div class="card-footer" v-if="mode==='edit' && editFocus===-1">
          <div >
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
import { Notification } from 'uiv'
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

interface PluginConf{
  readonly type:string
  config:any
}
interface ProjectPluginConfigEntry{
  entry:PluginConf
  extra:PluginConf
  validation:PluginValidation,
  create?:boolean
}
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
            pluginConfigs: [] as ProjectPluginConfigEntry[],
            configOrig:[] as any[],
            additionalProps: null as any,
            rundeckContext: {} as RundeckContext,
            modalAddOpen:false,
            pluginProviders:[],
            pluginLabels:{},
            editFocus:-1,
            errors: [] as string[]
        }
    },
    methods: {
      notifyError(msg:string, args:any[]) {
          Notification.notify({
            type: "danger",
            title: "An Error Occurred",
            content: msg,
            duration: 0
          })
      },

      notifySuccess(title:string, msg:string) {
        Notification.notify({
          type: "success",
          title: title,
          content: msg,
          duration: 5000
        });
      },
      createConfigEntry(entry:any): ProjectPluginConfigEntry{
        return {
          extra:{config:Object.assign({},entry.extra)},
          entry:{type:entry.type,config:Object.assign({},entry.config)}
        } as ProjectPluginConfigEntry
      },

      serializeConfigEntry(entry:ProjectPluginConfigEntry): any{
        return {
          extra: Object.assign({},entry.extra.config),
          type: entry.entry.type,
          config: Object.assign({},entry.entry.config)
        }
      },
      addPlugin(provider:string){
          this.pluginConfigs.push({entry:{type:provider,config:{}},extra:{},create:true} as ProjectPluginConfigEntry)
          this.setFocus(this.pluginConfigs.length-1)
      },
      setFocus(focus:number){
        this.editFocus=focus
      },
      async savePlugin(plugin:ProjectPluginConfigEntry,index:number){
        //validate
        const validation:PluginValidation = await pluginService.validatePluginConfig(this.serviceName, plugin.entry.type, plugin.entry.config)
        if(!validation.valid){
          Vue.set(plugin,'validation',validation)
          return
        }
        Vue.delete(plugin,'validation')
        plugin.create=false
        this.setFocus(-1)
      },
      removePlugin(plugin:ProjectPluginConfigEntry,index:string){
        const found=this.pluginConfigs.indexOf(plugin)
        this.pluginConfigs.splice(found,1)
        this.setFocus(-1)
      },

      movePlugin(index:number,plugin:ProjectPluginConfigEntry,shift:number){
        const found=this.pluginConfigs.indexOf(plugin)
        const item= this.pluginConfigs.splice(found,1)[0]
        const newindex=found+shift
        this.pluginConfigs.splice(newindex,0,item)
        this.editFocus=-1
      },
      async savePlugins () {
        try{
          const result= await this.saveProjectPluginConfig(this.project, this.configPrefix, this.serviceName, this.pluginConfigs)
          if(result.success){
            this.mode='show'
            this.notifySuccess("Success","Configuration Saved")
          }
        } catch (error){
          this.notifyError(error.message,[])
        }
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
        if (!response || response.status < 200 || response.status >= 300 ) {
          throw new Error(`Error reading project configuration for ${serviceName}: Response status: ${response? response.status:'None'}`)
        }
        if (response.data && response.data.plugins) {
            return response.data.plugins
        }
      },
      async saveProjectPluginConfig(project:string,configPrefix:string,serviceName:string,data:ProjectPluginConfigEntry[]){
        const resp = await client.sendRequest({
          url: `/framework/saveProjectPluginsAjax`,
          method: 'POST',
          queryParameters: {
                project: `${window._rundeck.projectName}`,
                configPrefix: this.configPrefix,
                serviceName: this.serviceName,
                },
          body: {plugins:data.map(this.serializeConfigEntry)}
        })

        if (resp && resp.status >= 200 && resp.status < 300 ) {
          return {success:true,data:resp.parsedBody}
        }
        if(resp && resp.status==422){
          //look for validation
          if(resp.parsedBody && resp.parsedBody.errors instanceof Array){
            this.errors = resp.parsedBody.errors
            if(resp.parsedBody.reports){
              const reports=resp.parsedBody.reports as {[key:string]:any}
              console.log("reports ",resp.parsedBody.reports)
              this.pluginConfigs.forEach((plugin,index)=>{
                  if(reports[`${index}`]!==undefined){
                      plugin.validation={valid:false,errors:reports[`${index}`]}
                  }
              })
            }
            return {success:false}
          }
        }
        throw new Error(`Error saving project configuration for ${serviceName}: Response status: ${resp? resp.status:'None'}`)
      },
      cancelAction(){
        this.pluginConfigs =  this.configOrig.map(this.createConfigEntry)
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
            this.additionalProps=window._rundeck.data['projectPluginConfigExtra']
            this.title=window._rundeck.data['title']||this.title

            this.loadProjectPluginConfig(window._rundeck.projectName,this.configPrefix,this.serviceName).then((pluginConfigs)=>{
              this.configOrig =  pluginConfigs
              //copy
              this.pluginConfigs = pluginConfigs.map(this.createConfigEntry)
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
