<template>
  <div>
    <div class="form-horizontal">
        <plugin-config
            v-if="isConfigSet"
            :mode="'edit'"
            :show-title="false"
            :show-description="false"
            :use-runner-selector="true"
            v-model="testConfig"
            :event-bus="rundeckContext.eventBus"
            :plugin-config="pluginConfig"
            :category="category"
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
      isConfigSet: false,
      testConfig: {config: {}}
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
        let properties = []
        this.extraConfigSet.forEach((item: ConfigurableItem) => {
          const itemName = item.name
          item.properties.forEach((prop: any) => {
            prop.name = itemName+prop.name
          })
          properties.push(item.properties)
        });
        let resolvedProps = []
        properties.forEach((item: any) => {
          item.forEach((prop: any) => {
            resolvedProps.push(prop)
          })
        })
        console.log("propeties", properties)
        this.pluginConfig = { type: 'ResourceModelSource', props: resolvedProps };
        this.isConfigSet = true;
        console.log("pluginConfig", this.pluginConfig)
      } catch (err) {
        console.error(err);
      }
    }
  },
  async mounted(){
    await this.loadConfig();
  }
});
</script>

<style>
</style>
