<template>
  <div class="help-block">
    {{
      $t("scheduledExecution.property.executionLifecyclePluginConfig.help.text")
    }}
  </div>
  <div class="list-group">
    <template
      v-for="(plugin, index) in executionLifecyclePlugins"
      :key="`plugin_${index}`"
    >
      <div class="list-group-item">
        <plugin-config
          v-if="plugin?.properties"
          v-model="plugin.extra"
          :config="plugin.extra.config"
          :mode="currentMode"
          service-name="ExecutionLifecycle"
          :provider="plugin.name"
          scope="Unspecified"
          default-scope="Unspecified"
          :show-icon="true"
        >
          <template #header="{ header }">
            <div class="checkbox">
              <input
                :id="`executionLifecyclePluginEnabled_${plugin.name}`"
                v-model="checkedData"
                :value="plugin.name"
                type="checkbox"
                class="form-control"
              />
              <label :for="`executionLifecyclePluginEnabled_${plugin.name}`">
                <pluginInfo
                  :show-title="header.inputShowTitle"
                  :show-icon="header.inputShowIcon"
                  :show-description="header.inputShowDescription"
                  :show-extended="true"
                  :detail="header.detail || plugin"
                >
                  <template #description>
                    <scheduled-execution-details
                      :allow-html="true"
                      :description="header.detail.desc || plugin.desc"
                      mode="collapsed"
                      :more-text="`More...`"
                    />
                  </template>
                </pluginInfo>
              </label>
            </div>
          </template>
        </plugin-config>
        <!--            <g:set-->
        <!--              var="prefix"-->
        <!--              value="executionLifecyclePlugins.${pluginKey}.configMap."-->
        <!--            />-->
        <!--            <g:render-->
        <!--              template="/framework/pluginConfigPropertiesInputs"-->
        <!--              model="${[-->
        <!--                                              service:ServiceNameConstants.ExecutionLifecycle,-->
        <!--                                              provider:pluginDescription.name,-->
        <!--                                              properties:pluginDescription?.properties,-->
        <!--                                              report: params.executionLifecyclePluginValidation?.get(pluginType),-->
        <!--                                              prefix:prefix,-->
        <!--                                              values:pluginConfig?:[:],-->
        <!--                                              fieldnamePrefix:prefix,-->
        <!--                                              origfieldnamePrefix:'orig.' + prefix,-->
        <!--                                              allowedScope:com.dtolabs.rundeck.core.plugins.configuration.PropertyScope.Instance-->
        <!--                                      ]}"-->
        <!--            />-->
      </div>
    </template>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import PluginInfo from "@/library/components/plugins/PluginInfo.vue";
import PluginConfig from "@/library/components/plugins/pluginConfig.vue";
import ScheduledExecutionDetails from "@/app/components/common/ScheduleExecutionDetails.vue";

interface PluginConf {
  readonly type: string;
  extra: any;
  config: any;
}
interface ProjectPluginConfigEntry {
  entry: PluginConf;
  extra: PluginConf;
  create?: boolean;
  originIndex?: number;
  index?: number;
  modified: boolean;
  readOnly: boolean;
}

export default defineComponent({
  name: "ExecutionEditor",
  components: { ScheduledExecutionDetails, PluginConfig, PluginInfo },
  props: ["modelValue"],
  data() {
    return {
      checkedData: [],
      executionLifecyclePlugins: [] as ProjectPluginConfigEntry[],
    };
  },
  computed: {
    // executionLifecyclePlugins() {
    //   // return this.modelValue?.executionLifecyclePlugins || [];
    //   if (this.modelValue?.executionLifecyclePlugins) {
    //     return this.modelValue?.executionLifecyclePlugins.map((e) =>
    //       this.massagePluginInfo(e),
    //     );
    //   }
    //   return [];
    // },
    prefix() {
      return `executionLifecyclePlugins.${this.pluginKey}.configMap.`;
    },
    currentMode() {
      return "create";
    },
  },
  watch: {
    modelValue: {
      deep: true,
      handler(newVal) {
        if (newVal && newVal?.executionLifecyclePluginConfigMap) {
          this.checkedData = Object.keys(
            newVal?.executionLifecyclePluginConfigMap,
          );

          this.executionLifecyclePlugins =
            newVal?.executionLifecyclePlugins.map((e) =>
              this.massagePluginInfo(e),
            );
        }
      },
    },
  },
  methods: {
    massagePluginInfo(plugin: object) {
      return {
        ...plugin,
        type: plugin.name,
        providerMetadata: plugin.metadata,
        desc: plugin.description,
        extra: {
          config: {},
        },
      };
    },
  },
});
</script>
