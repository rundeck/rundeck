<template>
  <div>
  <project-plugin-groups
    configPrefix="pluginValues.PluginGroup."
    service-name="PluginGroup"
    :edit-mode="true"
    :help="help"
    project=""
    @deleted="pluginsConfigWasDeleted"
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
    ProjectPluginGroups,
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
    eventBus: { type: Vue, required: false },
    configPrefix:String,
    project: String,
  },
  data(){
    return {
      pluginConfigs: [] as ProjectPluginConfigEntry[],
    }
  },
  computed:{
    exportedData():any[]{
      let data = [] as any
      let inputData = this.pluginConfigs
      inputData.forEach((plugin, index) => {
          data.push({type: plugin.entry.type, config: plugin.entry.config})
      });
      return data

    }
  },
  methods:{
    pluginsConfigWasSaved(pluginConfigs: ProjectPluginConfigEntry[]) {
      this.$emit("saved");
      this.pluginConfigs=pluginConfigs
    },
    pluginsConfigWasDeleted(pluginConfigs: ProjectPluginConfigEntry[]) {
      this.pluginConfigs=pluginConfigs
    },
    pluginsConfigWasModified() {
      this.$emit("modified");
    },
    pluginsConfigWasReset() {
      this.$emit("reset");
    }
  }
})

</script>
