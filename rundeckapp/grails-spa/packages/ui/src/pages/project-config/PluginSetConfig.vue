<template>
  <div>
  <project-plugin-groups
    configPrefix="pluginValues.PluginGroup."
    service-name="PluginGroup"
    :edit-mode="true"
    :help="help"
    project=""
    @saved="pluginsConfigWasSaved"
    @modified="pluginsConfigWasModified"
    @reset="pluginsConfigWasReset"
    :edit-button-text="$t('Edit Plugin Groups')"
    :mode-toggle="modeToggle"
  >
  </project-plugin-groups>
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
import PluginValidation from "@rundeck/ui-trellis/lib/interfaces/PluginValidation";
import ProjectPluginGroups from "./ProjectPluginGroups.vue";

const client: RundeckBrowser = getRundeckContext().rundeckClient
const rdBase = getRundeckContext().rdBase

interface PluginConf {
  readonly type: string;
  config: any;
}
interface ProjectPluginConfigEntry {
  entry: PluginConf;
  extra: PluginConf;
  validation: PluginValidation;
  create?: boolean;
  origIndex?: number;
  modified: boolean;
}
export default Vue.extend({
  name: "App",
  components: {
    PluginInfo,
    PluginConfig,
    ProjectPluginGroups,
    Expandable
  },
  props:{
    serviceName:String,
    help: {
      type: String,
      required: false
    },
    modeToggle: {
      type: Boolean,
      default: true
    },
    showWriteableLinkMode: {
      type: String,
      default: "show"
    },
    // isWriteable(index: number): boolean {
    //   return (
    //     this.pluginProviders.length > index &&
    //     this.pluginProviders[index].resources.writeable
    //   );
    // },
    // editPermalink(index: number): string | undefined {
    //   return this.sourcesData.length > index &&
    //   this.sourcesData[index].resources.editPermalink
    //     ? this.sourcesData[index].resources.editPermalink
    //     : "#";
    // },
    // sourceErrors(index: number): string | undefined {
    //   return this.sourcesData.length > index
    //     ? this.sourcesData[index].errors
    //     : undefined;
    // },
    eventBus: { type: Vue, required: false },
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
      pluginConfigs: [] as ProjectPluginConfigEntry[],
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
      let data = [] as any
      let inputData = this.pluginConfigs
      inputData.forEach((plugin, index) => {
          data.push({type: plugin.entry.type, config: plugin.entry.config})
      });
      console.log(data)
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
    pluginsConfigWasSaved(pluginConfigs: ProjectPluginConfigEntry[]) {
      this.$emit("saved");
      this.pluginConfigs=pluginConfigs
    },
    pluginsConfigWasModified() {
      this.$emit("modified");
    },
    pluginsConfigWasReset() {
      this.$emit("reset");
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
    createConfigEntry(entry: any, origIndex: number): ProjectPluginConfigEntry {
      return {
        extra: { config: Object.assign({}, entry.extra) },
        entry: { type: entry.type, config: Object.assign({}, entry.config) },
        origIndex: origIndex
      } as ProjectPluginConfigEntry;
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

    const projectPluginConfigList = [] as ProjectPluginConfigEntry[]
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
            // console.log("Made it here")
            // console.log(index)
            // console.log(this.pluginProviders)
          });
        }

      }).catch(error => console.error(error));
  }
})

</script>
