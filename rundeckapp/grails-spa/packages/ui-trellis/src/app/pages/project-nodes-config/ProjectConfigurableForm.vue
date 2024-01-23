<template>
  <div>
    <div class="form-horizontal">
        <plugin-config
            :mode="'edit'"
            :serviceName="this.category"
            :show-title="false"
            :show-description="false"
            :use-runner-selector="true"
            :event-bus="this.rundeckContext.eventBus"
            :plugin-config="this.pluginConfig"
            :category="this.category"
        />
    </div>
  </div>
</template>

<script lang="ts">
import PluginConfig from "../../../library/components/plugins/pluginConfig.vue"
import {getRundeckContext, RundeckContext} from "../../../library";
import {getProjectConfigurable} from "./nodeSourcesUtil";
import {defineComponent, onMounted, ref} from "vue";
interface ConfigurableItem{
  name?:string
  properties?:[{}]
  propertiesMapping?:any
  values?:any
}


export default defineComponent({
  name: "ProjectConfigurableForm",
  components: {
    PluginConfig,
  },
  data() {
    return {
      rundeckContext: {} as RundeckContext,
      pluginConfig: {},
      extraConfigSet: {},
    };
  },
  props: ['category', 'categoryPrefix', 'helpCode', 'serviceName'],
  methods: {
    async loadConfig() {
      console.log("Loading config");
      try {
        const config = await getProjectConfigurable(window._rundeck.projectName, this.category);
        console.log(config)
        this.extraConfigSet =  config.response["projectConfigurable"] as [ConfigurableItem];
        const properties = []
        this.extraConfigSet.forEach((item: ConfigurableItem) => {
          properties.push(item.properties)
        });
        console.log("propeties", properties)
        this.pluginConfig = { type: 'ResourceModelSource', props: properties };
        console.log("pluginConfig", this.pluginConfig)
      } catch (err) {
        console.error(err);
      }
    }
  },
  async beforeMount(){
    await this.loadConfig();
  }
});
</script>

<style>
</style>
