<template>
  <div>
    <div class="form-horizontal">
        <plugin-config
            v-if="isConfigSet"
            :mode="'create'"
            :show-title="false"
            :show-description="false"
            :use-runner-selector="true"
            v-model="finalExtraConfig"
            :event-bus="rundeckContext.eventBus"
            :plugin-config="pluginConfig"
            :category="category"
        >
          <template v-slot:extra>
            <div class="row">
              <div class="col-xs-12 col-sm-12">
                    <span>
                        <a
                            class="btn btn-cta btn-xs"
                            @click="saveConfig"
                            :key="'save'"
                        >{{"Save"}}</a>
                    </span>
              </div>
            </div>
          </template>
        </plugin-config>
    </div>
  </div>
</template>

<script lang="ts">
import PluginConfig from "../../../library/components/plugins/pluginConfig.vue"
import {getRundeckContext, RundeckContext} from "../../../library";
import {getProjectConfigurable, setProjectConfigurable} from "./nodeSourcesUtil";
import {Notification} from "uiv"
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
      projectConfigurableNames: [],
      finalExtraConfig: {config: {}},
      originalConfig: {},
    };
  },
  props: ['category', 'categoryPrefix', 'helpCode', 'serviceName'],
  methods: {
    async loadConfig() {
      try {
        const config = await getProjectConfigurable(window._rundeck.projectName, this.category);
        console.log(config)
        this.extraConfigSet =  config.response["projectConfigurable"] as [ConfigurableItem];
        let properties = []
        this.extraConfigSet.forEach((item: ConfigurableItem) => {
          console.log("item", item)
          const itemName = item.name
          this.projectConfigurableNames.push(itemName)
          item.properties.forEach((prop: any) => {
            const originalPropName = prop.name
            prop.name = itemName+ "." + prop.name
            Object.entries(item.values).forEach(([key, value]) => {
              if(originalPropName === key){
                this.originalConfig[prop.name] = value
              }
            })
          })
          properties.push(item.properties)
        });
        let resolvedProps = []
        properties.forEach((item: any) => {
          item.forEach((prop: any) => {
            prop.desc = prop.description
            resolvedProps.push(prop)
          })
        })
        this.pluginConfig = { type: 'ResourceModelSource', props: resolvedProps };
        this.finalExtraConfig.config = this.originalConfig;
        this.isConfigSet = true;
      } catch (err) {
        console.error(err);
      }
    },
    async saveConfig(){

      console.log("Saving config", this.finalExtraConfig.config)
      const configMap = this.transformConfigToMap(this.finalExtraConfig.config);
      this.finalExtraConfig.config = {
        ...this.finalExtraConfig.config,
        ...configMap
      };
      this.finalExtraConfig.config = this.convertMapNumbersToStrings(this.finalExtraConfig.config);
      console.log("Saving config 2", this.finalExtraConfig.config)
      try {
        const resp = await setProjectConfigurable(window._rundeck.projectName, this.category, this.finalExtraConfig.config);
        console.log(resp)
        if(resp.response === true){
          console.log("Config saved successfully")
          this.notifySuccess("Success!", "Config saved successfully" )
          await this.loadConfig()
        }
      } catch (err) {
        console.error(err);
        this.notifyError( "Error saving config: " + err.message)
      }
    },
    convertMapNumbersToStrings(obj: any): any {
      for (const key in obj) {
        if (typeof obj[key] === 'number') {
          // Convert number to string
          obj[key] = obj[key].toString();
        } else if (typeof obj[key] === 'object' && obj[key] !== null) {
          // Recursively convert numbers to strings in nested objects
          obj[key] = this.convertMapNumbersToStrings(obj[key]);
        }
      }
      return obj;
    },
    transformConfigToMap(config: any) {
      const resultMap = {};

      Object.entries(config).forEach(([key, value]) => {
        const [mainKey, subKey] = key.split('.');
        if (!resultMap[mainKey]) {
          resultMap[mainKey] = {};
        }
        resultMap[mainKey][subKey] = value;
      });

      return resultMap;
    },
    notifyError(msg: string) {
      Notification.notify({
        type: "danger",
        title: "An Error Occurred",
        content: msg,
        duration: 0
      });
    },

    notifySuccess(title: string, msg: string) {
      Notification.notify({
        type: "success",
        title: title,
        content: msg,
        duration: 5000
      });
    },
  },
  async mounted(){
    await this.loadConfig();
  }
});
</script>

<style>
</style>
