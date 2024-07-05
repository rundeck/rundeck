<template>
  <div class="help-block">
    {{
      $t("scheduledExecution.property.executionLifecyclePluginConfig.help.text")
    }}
  </div>
  <!--  <div class="list-group">-->
  <!--    <template v-for="plugin in executionLifecyclePlugins">-->
  <!--      <input-->
  <!--        type="hidden"-->
  <!--        name="executionLifecyclePlugins.keys"-->
  <!--        :value="pluginKey"-->
  <!--      />-->
  <!--      <input-->
  <!--        type="hidden"-->
  <!--        :name="`executionLifecyclePlugins.type.${pluginKey}`"-->
  <!--        :value="pluginType"-->
  <!--      />-->
  <!--      <div class="list-group-item">-->
  <!--        <template v-if="pluginDescription">-->
  <!--          <div class="form-group">-->
  <!--            <div class="col-sm-12">-->
  <!--              <div class="checkbox">-->
  <!--                <g:set var="prkey" value="${rkey()}" />-->
  <!--                <input-->
  <!--                  id="calendarEnabled"-->
  <!--                  type="checkbox"-->
  <!--                  class="form-control"-->
  <!--                />-->
  <!--                <g:checkBox-->
  <!--                  id="executionLifecyclePluginEnabled_${prkey}"-->
  <!--                  name="executionLifecyclePlugins.enabled.${pluginKey}"-->
  <!--                  value="true"-->
  <!--                  class="form-control"-->
  <!--                  checked="${pluginConfig != null}"-->
  <!--                />-->

  <!--                <label :for="executionLifecyclePluginEnabled_${prkey}">-->
  <!--                  <plugin-info>-->
  <!--                    &lt;!&ndash;                    <scheduled-execution-description />&ndash;&gt;-->
  <!--                  </plugin-info>-->
  <!--                  <g:render-->
  <!--                    template="/framework/renderPluginDesc"-->
  <!--                    model="${[-->
  <!--                                                      serviceName    : ServiceNameConstants.ExecutionLifecycle,-->
  <!--                                                      description    : pluginDescription,-->
  <!--                                                      showPluginIcon : true,-->
  <!--                                                      showNodeIcon   : false,-->
  <!--                                                      hideTitle      : false,-->
  <!--                                                      hideDescription: false,-->
  <!--                                                      fullDescription: false-->
  <!--                                              ]}"-->
  <!--                  />-->
  <!--                </label>-->
  <!--              </div>-->
  <!--            </div>-->
  <!--          </div>-->
  <!--          <template v-if="pluginDescription?.properties">-->
  <!--            <g:set-->
  <!--              var="prefix"-->
  <!--              value="executionLifecyclePlugins.${pluginKey}.configMap."-->
  <!--            />-->
  <!--            <g:render-->
  <!--              template="/framework/pluginConfigPropertiesInputs"-->
  <!--              model="${[-->
  <!--                                          service:ServiceNameConstants.ExecutionLifecycle,-->
  <!--                                          provider:pluginDescription.name,-->
  <!--                                          properties:pluginDescription?.properties,-->
  <!--                                          report: params.executionLifecyclePluginValidation?.get(pluginType),-->
  <!--                                          prefix:prefix,-->
  <!--                                          values:pluginConfig?:[:],-->
  <!--                                          fieldnamePrefix:prefix,-->
  <!--                                          origfieldnamePrefix:'orig.' + prefix,-->
  <!--                                          allowedScope:com.dtolabs.rundeck.core.plugins.configuration.PropertyScope.Instance-->
  <!--                                  ]}"-->
  <!--            />-->
  <!--          </template>-->
  <!--        </template>-->
  <!--      </div>-->
  <!--    </template>-->
  <!--    <g:each in="${executionLifecyclePlugins}" var="plugin">-->
  <!--      <g:set-->
  <!--        var="pluginKey"-->
  <!--        value="${params.executionLifecyclePlugin?.type?.get(pluginType)?:g.rkey()}"-->
  <!--      />-->
  <!--      <g:set var="pluginType" value="${plugin.key}" />-->
  <!--      <g:hiddenField-->
  <!--        name="executionLifecyclePlugins.keys"-->
  <!--        value="${pluginKey}"-->
  <!--      />-->
  <!--      <g:hiddenField-->
  <!--        name="executionLifecyclePlugins.type.${pluginKey}"-->
  <!--        value="${pluginType}"-->
  <!--      />-->
  <!--      <g:set var="pluginDescription" value="${plugin.value.description}" />-->
  <!--      <g:set-->
  <!--        var="pluginConfig"-->
  <!--        value="${params.executionLifecyclePlugins?.get(pluginKey)?.configMap ?: executionLifecyclePluginConfigMap[pluginType]}"-->
  <!--      />-->

  <!--      <div class="list-group-item">-->
  <!--        <g:if test="${pluginDescription}">-->
  <!--          <div class="form-group">-->
  <!--            <div class="col-sm-12">-->
  <!--              <div class="checkbox">-->
  <!--                <g:set var="prkey" value="${rkey()}" />-->
  <!--                <input-->
  <!--                  id="calendarEnabled"-->
  <!--                  type="checkbox"-->
  <!--                  class="form-control"-->
  <!--                />-->
  <!--                <g:checkBox-->
  <!--                  id="executionLifecyclePluginEnabled_${prkey}"-->
  <!--                  name="executionLifecyclePlugins.enabled.${pluginKey}"-->
  <!--                  value="true"-->
  <!--                  class="form-control"-->
  <!--                  checked="${pluginConfig != null}"-->
  <!--                />-->

  <!--                <label for="executionLifecyclePluginEnabled_${prkey}">-->
  <!--                  <g:render-->
  <!--                    template="/framework/renderPluginDesc"-->
  <!--                    model="${[-->
  <!--                                                      serviceName    : ServiceNameConstants.ExecutionLifecycle,-->
  <!--                                                      description    : pluginDescription,-->
  <!--                                                      showPluginIcon : true,-->
  <!--                                                      showNodeIcon   : false,-->
  <!--                                                      hideTitle      : false,-->
  <!--                                                      hideDescription: false,-->
  <!--                                                      fullDescription: false-->
  <!--                                              ]}"-->
  <!--                  />-->
  <!--                </label>-->
  <!--              </div>-->
  <!--            </div>-->
  <!--          </div>-->
  <!--          <template v-if="pluginDescription?.properties">-->
  <!--            <g:set-->
  <!--              var="prefix"-->
  <!--              value="executionLifecyclePlugins.${pluginKey}.configMap."-->
  <!--            />-->
  <!--            <plugin-config :provider="pluginDescription.name" />-->
  <!--            <g:render-->
  <!--              template="/framework/pluginConfigPropertiesInputs"-->
  <!--              model="${[-->
  <!--                                          service:ServiceNameConstants.ExecutionLifecycle,-->
  <!--                                          provider:pluginDescription.name,-->
  <!--                                          properties:pluginDescription?.properties,-->
  <!--                                          report: params.executionLifecyclePluginValidation?.get(pluginType),-->
  <!--                                          prefix:prefix,-->
  <!--                                          values:pluginConfig?:[:],-->
  <!--                                          fieldnamePrefix:prefix,-->
  <!--                                          origfieldnamePrefix:'orig.' + prefix,-->
  <!--                                          allowedScope:com.dtolabs.rundeck.core.plugins.configuration.PropertyScope.Instance-->
  <!--                                  ]}"-->
  <!--            />-->
  <!--          </template>-->
  <!--        </g:if>-->
  <!--      </div>-->
  <!--    </g:each>-->
  <!--  </div>-->
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  name: "ExecutionEditor",
  props: ["modelValue"],
  computed: {
    // pluginKey() {
    //   return this.modelValue.executionLifecyclePlugin?.type?
    // },
  },
});
</script>
