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
                    :mode="'edit'"
                    :serviceName="serviceName"
                    v-model="pluginData[provider.name]"
                    :provider="provider.name"
                    :config="pluginData[provider.name]"
                    :key="provider.name+'_config/'+index"
                    :show-title="false"
                    :show-description="false"
                    :validation="pluginValidation[provider.name]"
                    :validation-warning-text="$t('Validation errors')"

            />
            <btn v-if="typeof(pluginData[provider.name])=='undefined'" @click="addConfig(provider.name)">
                Add
                    <i class="glyphicon glyphicon-plus text-muted"></i>
            </btn>
            <btn v-if="typeof(pluginData[provider.name])!=='undefined'" @click="removeConfig(provider.name)">
                Remove
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
import axios from "axios";
import Vue from "vue";
import { Notification } from "uiv";
import { getRundeckContext, RundeckContext } from "@rundeck/ui-trellis";
import Expandable from "@rundeck/ui-trellis/lib/components/utils/Expandable.vue";
import PluginInfo from "@rundeck/ui-trellis/lib/components/plugins/PluginInfo.vue";
import PluginConfig from "@rundeck/ui-trellis/lib/components/plugins/pluginConfig.vue";
import pluginService from "@rundeck/ui-trellis/lib/modules/pluginService";
import PluginValidation from "@rundeck/ui-trellis/lib/interfaces/PluginValidation";

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
    configPrefix:String
  },
  data(){
    return {
      pluginProviders:[],
      pluginLabels:{},
      pluginData:{} as {[key:string]:PluginConf},
      pluginValidation:{}
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
    async addConfig(name:string){
      Vue.set(this.pluginData,name,{
        type:name,
        config:{}
      })
    },
    async removeConfig(name:string){
      Vue.delete(this.pluginData,name)
    }
  },
  async mounted() {
    (this.configList as PluginConf[])?.forEach((val: PluginConf) => this.pluginData[val.type] = val)

    pluginService
      .getPluginProvidersForService(this.serviceName)
      .then(data => {
        if (data.service) {
          this.pluginProviders = data.descriptions;
          this.pluginLabels = data.labels;
        }
      }).catch(error => console.error(error));
  }
})

</script>