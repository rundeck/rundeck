<template>
  <div class="help-block">
    {{
      $t("scheduledExecution.property.executionLifecyclePluginConfig.help.text")
    }}
  </div>
  <div
    v-for="(plugin, index) in executionLifecyclePlugins"
    :key="`plugin_${index}`"
    class="list-group"
  >
    <input
      type="hidden"
      name="executionLifecyclePlugins.keys"
      :value="pluginKey"
    />
    <input
      type="hidden"
      :name="`executionLifecyclePlugins.type.${pluginKey}`"
      :value="pluginType"
    />
    <div class="list-group-item">
      <div class="form-group">
        <div class="col-sm-12">
          <div class="checkbox">
            <!--                <g:set var="prkey" value="${rkey()}" />-->
            <input
              :id="`executionLifecyclePluginEnabled_${prkey}`"
              :name="`executionLifecyclePlugins.enabled.${pluginKey}`"
              type="checkbox"
              class="form-control"
            />
            <!--                <g:checkBox-->
            <!--                  id="executionLifecyclePluginEnabled_${prkey}"-->
            <!--                  name="executionLifecyclePlugins.enabled.${pluginKey}"-->
            <!--                  value="true"-->
            <!--                  class="form-control"-->
            <!--                  checked="${pluginConfig != null}"-->
            <!--                />-->

            <label :for="`executionLifecyclePluginEnabled_${prkey}`">
              <plugin-info
                :show-title="true"
                :show-icon="true"
                :show-extended="true"
                :show-description="true"
                :detail="plugin"
              >
                <template #description>
                  <scheduled-execution-details
                    :allow-html="true"
                    :description="plugin.description"
                    mode="collapsed"
                  />
                </template>
              </plugin-info>
              <!--                  <g:render-->
              <!--                    template="/framework/renderPluginDesc"-->
              <!--                    model="${[-->
              <!--                                                          serviceName    : ServiceNameConstants.ExecutionLifecycle,-->
              <!--                                                          description    : pluginDescription,-->
              <!--                                                          showPluginIcon : true,-->
              <!--                                                          showNodeIcon   : false,-->
              <!--                                                          hideTitle      : false,-->
              <!--                                                          hideDescription: false,-->
              <!--                                                          fullDescription: false-->
              <!--                                                  ]}"-->
              <!--                  />-->
            </label>
          </div>
        </div>
      </div>
      <template v-if="plugin?.properties">
        <plugin-config
          mode="edit"
          service-name="ExecutionLifecycle"
          :provider="plugin.name"
          :model-value="test"
          :plugin-config="plugin.properties"
        ></plugin-config>
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
      </template>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import PluginInfo from "@/library/components/plugins/PluginInfo.vue";
import PluginConfig from "@/library/components/plugins/pluginConfig.vue";
import ScheduledExecutionDetails from "@/app/components/common/ScheduleExecutionDetails.vue";

export default defineComponent({
  name: "ExecutionEditor",
  components: { ScheduledExecutionDetails, PluginConfig, PluginInfo },
  props: ["modelValue"],
  data() {
    return {
      // executionLifecyclePlugins: [{}],
      test: {
        config: {},
      },
    };
  },
  computed: {
    executionLifecyclePlugins() {
      // return this.modelValue?.executionLifecyclePlugins || [];
      if (this.modelValue?.executionLifecyclePlugins) {
        return this.modelValue?.executionLifecyclePlugins.map((e) =>
          this.massagePluginInfo(e),
        );
      }
      return [];
    },
    pluginKey() {
      // return this.modelValue.executionLifecyclePlugin?.type?
      return "";
    },
    pluginType() {
      return "";
    },
    pluginDescription() {
      return { properties: [] };
    },
    prkey() {
      return "";
    },
    prefix() {
      return `executionLifecyclePlugins.${this.pluginKey}.configMap.`;
    },
  },
  methods: {
    massagePluginInfo(plugin: object) {
      return {
        ...plugin,
        // key: e9df7b5f,
        type: plugin.name,
        providerMetadata: plugin.metadata,
      };
    },
  },
});
</script>
