<template>
  <div class="help-block">
    {{
      $t("scheduledExecution.property.executionLifecyclePluginConfig.help.text")
    }}
  </div>
  <!--  <div-->
  <!--    v-for="(plugin, index) in executionLifecyclePlugins"-->
  <!--    :key="`plugin_${index}`"-->
  <!--    class="list-group"-->
  <!--  >-->
  <!--    <input-->
  <!--      type="hidden"-->
  <!--      name="executionLifecyclePlugins.keys"-->
  <!--      :value="pluginKey"-->
  <!--    />-->
  <!--    <input-->
  <!--      type="hidden"-->
  <!--      :name="`executionLifecyclePlugins.type.${pluginKey}`"-->
  <!--      :value="pluginType"-->
  <!--    />-->
  <!--    <div class="list-group-item">-->
  <!--      <template v-if="pluginDescription">-->
  <!--        <div class="form-group">-->
  <!--          <div class="col-sm-12">-->
  <!--            <div class="checkbox">-->
  <!--              &lt;!&ndash;                <g:set var="prkey" value="${rkey()}" />&ndash;&gt;-->
  <!--              <input-->
  <!--                :id="`executionLifecyclePluginEnabled_${prkey}`"-->
  <!--                :name="`executionLifecyclePlugins.enabled.${pluginKey}`"-->
  <!--                type="checkbox"-->
  <!--                class="form-control"-->
  <!--              />-->
  <!--              &lt;!&ndash;                <g:checkBox&ndash;&gt;-->
  <!--              &lt;!&ndash;                  id="executionLifecyclePluginEnabled_${prkey}"&ndash;&gt;-->
  <!--              &lt;!&ndash;                  name="executionLifecyclePlugins.enabled.${pluginKey}"&ndash;&gt;-->
  <!--              &lt;!&ndash;                  value="true"&ndash;&gt;-->
  <!--              &lt;!&ndash;                  class="form-control"&ndash;&gt;-->
  <!--              &lt;!&ndash;                  checked="${pluginConfig != null}"&ndash;&gt;-->
  <!--              &lt;!&ndash;                />&ndash;&gt;-->

  <!--              <label :for="`executionLifecyclePluginEnabled_${prkey}`">-->
  <!--                <plugin-info :detail="{}">-->
  <!--                  &lt;!&ndash;                    <scheduled-execution-description />&ndash;&gt;-->
  <!--                </plugin-info>-->
  <!--                &lt;!&ndash;                  <g:render&ndash;&gt;-->
  <!--                &lt;!&ndash;                    template="/framework/renderPluginDesc"&ndash;&gt;-->
  <!--                &lt;!&ndash;                    model="${[&ndash;&gt;-->
  <!--                &lt;!&ndash;                                                          serviceName    : ServiceNameConstants.ExecutionLifecycle,&ndash;&gt;-->
  <!--                &lt;!&ndash;                                                          description    : pluginDescription,&ndash;&gt;-->
  <!--                &lt;!&ndash;                                                          showPluginIcon : true,&ndash;&gt;-->
  <!--                &lt;!&ndash;                                                          showNodeIcon   : false,&ndash;&gt;-->
  <!--                &lt;!&ndash;                                                          hideTitle      : false,&ndash;&gt;-->
  <!--                &lt;!&ndash;                                                          hideDescription: false,&ndash;&gt;-->
  <!--                &lt;!&ndash;                                                          fullDescription: false&ndash;&gt;-->
  <!--                &lt;!&ndash;                                                  ]}"&ndash;&gt;-->
  <!--                &lt;!&ndash;                  />&ndash;&gt;-->
  <!--              </label>-->
  <!--            </div>-->
  <!--          </div>-->
  <!--        </div>-->
  <!--        <template v-if="pluginDescription?.properties">-->
  <!--          &lt;!&ndash;            <g:set&ndash;&gt;-->
  <!--          &lt;!&ndash;              var="prefix"&ndash;&gt;-->
  <!--          &lt;!&ndash;              value="executionLifecyclePlugins.${pluginKey}.configMap."&ndash;&gt;-->
  <!--          &lt;!&ndash;            />&ndash;&gt;-->
  <!--          &lt;!&ndash;            <g:render&ndash;&gt;-->
  <!--          &lt;!&ndash;              template="/framework/pluginConfigPropertiesInputs"&ndash;&gt;-->
  <!--          &lt;!&ndash;              model="${[&ndash;&gt;-->
  <!--          &lt;!&ndash;                                              service:ServiceNameConstants.ExecutionLifecycle,&ndash;&gt;-->
  <!--          &lt;!&ndash;                                              provider:pluginDescription.name,&ndash;&gt;-->
  <!--          &lt;!&ndash;                                              properties:pluginDescription?.properties,&ndash;&gt;-->
  <!--          &lt;!&ndash;                                              report: params.executionLifecyclePluginValidation?.get(pluginType),&ndash;&gt;-->
  <!--          &lt;!&ndash;                                              prefix:prefix,&ndash;&gt;-->
  <!--          &lt;!&ndash;                                              values:pluginConfig?:[:],&ndash;&gt;-->
  <!--          &lt;!&ndash;                                              fieldnamePrefix:prefix,&ndash;&gt;-->
  <!--          &lt;!&ndash;                                              origfieldnamePrefix:'orig.' + prefix,&ndash;&gt;-->
  <!--          &lt;!&ndash;                                              allowedScope:com.dtolabs.rundeck.core.plugins.configuration.PropertyScope.Instance&ndash;&gt;-->
  <!--          &lt;!&ndash;                                      ]}"&ndash;&gt;-->
  <!--          &lt;!&ndash;            />&ndash;&gt;-->
  <!--        </template>-->
  <!--      </template>-->
  <!--    </div>-->
  <!--  </div>-->
</template>

<script lang="ts">
import { defineComponent } from "vue";
import PluginInfo from "@/library/components/plugins/PluginInfo.vue";

export default defineComponent({
  name: "ExecutionEditor",
  components: { PluginInfo },
  props: ["modelValue"],
  data() {
    return {
      executionLifecyclePlugins: [{}],
    };
  },
  computed: {
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
});
</script>
