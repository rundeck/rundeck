<template>
<div>

<div class="list-group" v-if="pluginProviders.length>0">
  <div
    class="list-group-item"
    v-for="(provider,index) in pluginProviders"
    :key="'projectGroupPlugin_'+index"
  >

    <plugin-config
      :mode="'title'"
      :serviceName="serviceName"
      :provider="provider.name"
      :key="provider.name+'title/'+index"
      :show-description="true"
    >
    </plugin-config>


    <plugin-config
      v-if="typeof(pluginData[provider.name])!=='undefined'"
      :mode="editMode[provider.name]===true?'edit':'show'"
      :serviceName="serviceName"
      v-model="pluginData[provider.name]"
      :provider="provider.name"
      :config="projectSettings[provider.name]"
      :key="provider.name+'_config/'+index+updateKey"
      :show-title="false"
      :show-description="false"
      :validation="pluginValidation[provider.name]"
      :validation-warning-text="$t('Validation errors')"

    />
    <btn v-if="typeof(pluginData[provider.name])=='undefined'" @click="addConfig(provider.name, serviceName)">
      Add
      <i class="glyphicon glyphicon-plus text-muted"></i>
    </btn>
    <btn v-if="typeof(pluginData[provider.name])!=='undefined'" @click="removeConfig(provider.name)">
      Remove
      <i class="glyphicon glyphicon-mins text-muted"></i>
    </btn>
    <btn v-if="typeof(pluginData[provider.name])!=='undefined' && !showSave[provider.name]" @click="addConfig(provider.name)">
      Edit
      <i class="glyphicon glyphicon-mins text-muted"></i>
    </btn>
    <btn v-if="typeof(pluginData[provider.name])!=='undefined' && showSave[provider.name]" @click="saveConfig(provider.name, serviceName)">
      Close
      <i class="glyphicon glyphicon-mins text-muted"></i>
    </btn>

  </div>
</div>
<template v-if="exportedData">
  <input type="hidden" :value="JSON.stringify(exportedData)" :name="configPrefix+'json'"/>
</template>
</div>
</template>
<script lang="ts">
import Vue from "vue";
import {getRundeckContext} from "@rundeck/ui-trellis";
import Expandable from "@rundeck/ui-trellis/lib/components/utils/Expandable.vue";
import PluginInfo from "@rundeck/ui-trellis/lib/components/plugins/PluginInfo.vue";
import PluginConfig from "@rundeck/ui-trellis/lib/components/plugins/pluginConfig.vue";
import pluginService from "@rundeck/ui-trellis/lib/modules/pluginService";
import {RundeckBrowser} from "@rundeck/client";

const client: RundeckBrowser = getRundeckContext().rundeckClient
const rdBase = getRundeckContext().rdBase

interface PluginConf {
  readonly type: string;
  config: any;
}
export default Vue.extend({
  name: "App",
  components: {
    PluginInfo,
    PluginConfig,
    Expandable
  },
  props:{
    serviceName:String,
    configList: {default:[],type:Array },
    EventBus:Object,
    configPrefix:String,
    project: String,
  },
  data(){
    return {
      pluginProviders:[] as any,
      pluginLabels:{},
      pluginData:{} as {[key:string]:PluginConf},
      pluginValidation:{},
      projectSettings: {},
      pluginGroupSettings: {},
      editMode: {} as any,
      showSave: {} as any,
      updateKey: Date.now(),
      buttonName: '',
      testString: ''
    }
  },
  computed:{
    exportedData():any[]{
      let data = []
      let inputData = this.pluginData
      for (let key in inputData) {
        data.push({type: key, config: inputData[key].config})
      }
      return data
    }
  },
  methods:{
    async addConfig(name:string, service:string){
      this.editMode[name]=true
      this.showSave[name]=true
      this.updateKey = Date.now()

      await this.setConfig(name, service)
    },
    async setConfig(name: string, service:string){
      const pluginPath = "project.plugin.PluginGroup." + name +"."

      let config ={} as any

      Object.keys(this.projectSettings).forEach((key: string)=>{
        if(key.includes(pluginPath)){
          config[key.replace(pluginPath,"")]=(this.projectSettings as any)[key]
        }
      });

      this.pluginValidation=await pluginService.validatePluginConfig(
        service,
        name,
        config,
        'Project'
      )

      Vue.set(this.pluginData,name,{
        type:name,
        config:config
      })
    },
    async saveConfig(name:string, service:string){
      this.editMode[name]=false
      this.showSave[name]=false
      this.updateKey = Date.now()
    },
    async removeConfig(name:string){
      Vue.delete(this.pluginData,name)
    },
    async getProjectProperties(){
      let result = await client.sendRequest(
        {
          baseUrl: rdBase,
          pathTemplate: "/api/"+window._rundeck.apiVersion+"/project/"+this.project+"/config",
          method: "GET",
          headers: {"content-type": "application/json"}
        }
      )

      if (result.status == 200) {
        this.projectSettings = result.parsedBody
      }
    }
  },
  async mounted() {
    (this.configList as PluginConf[])?.forEach((val: PluginConf) => this.pluginData[val.type] = val)
    //this.editMode(false)
    //this.editMode=false

    await this.getProjectProperties()
    pluginService
      .getPluginProvidersForService(this.serviceName)
      .then(data => {
        if (data.service) {
          this.pluginProviders = data.descriptions;
          this.pluginLabels = data.labels;
          this.pluginProviders.forEach((provider: any, index: any)=>{
            this.pluginProviders[index]["created"]=false
            Object.keys(this.projectSettings).forEach((key2: string)=>{
              if(key2.includes("project.plugin.PluginGroup." + provider.name)){
                this.pluginProviders[index]["created"]=true
                this.setConfig(provider.name, this.serviceName)
              }
            });
            console.log("Made it here")
            console.log(index)
            console.log(this.pluginProviders)
          });
        }

      }).catch(error => console.error(error));
  }
})

</script>
