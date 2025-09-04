<template>
  <div class="step-card-content">
    <plugin-config
      class="plugin-config-section"
      serviceName="WorkflowStep"
      provider="pagerduty-create-user"
      :config="pdStep.config"
      :readOnly="true"
      :showTitle="false"
      :showIcon="false"
      :showDescription="false"
      mode="show"
      allowCopy
    />
    <ConfigSection
      title="Log Filters"
      tooltip="Filters that will affect the logs produces by these steps"
      v-model="logFilters"
      @addElement="handleAddElement"
    />
    <ConfigSection
      title="Error Handler"
      tooltip="In case of error, the following step will be run"
      v-model="errorHandler"
      @addElement="handleAddErrorHandler"
      hideWhenSingle
      class="error-handler"
    >
      <template #header v-if="errorHandler.length >= 1">
        <plugin-info
          :detail="{ title: 'Command' }"
          :show-description="false"
          :show-extended="false"
          :show-icon="false"
          titleCss="link-title"
        />
      </template>
      <template #extra v-if="errorHandler.length >= 1">
        <plugin-config
          class="plugin-config-section"
          serviceName="WorkflowNodeStep"
          provider="exec-command"
          :config="{ adhocRemoteString: 'echo error happened' }"
          :readOnly="true"
          :showTitle="false"
          :showIcon="false"
          :showDescription="false"
          mode="show"
          allowCopy
        />
      </template>
    </ConfigSection>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import ConfigSection from "@/library/components/primeVue/StepCards/ConfigSection.vue";
import PluginConfig from "@/library/components/plugins/pluginConfig.vue";
import PluginInfo from "@/library/components/plugins/PluginInfo.vue";

export default defineComponent({
  name: "StepCardContent",
  components: {
    PluginInfo,
    PluginConfig,
    ConfigSection,
  },
  props: {},
  data() {
    return {
      logFilters: [],
      errorHandler: [],
      pdStep: {
        description: "Add new responder in PD",
        nodeStep: false,
        jobref: undefined,
        type: "pagerduty-create-user",
        config: {
          email: "janedoe@pagerduty.com",
          name: "Jane Doe",
          color: "green",
          role: "admin",
          title: "Responder",
          description:
            "Loves uneventful days, but always ready to hop on a call to help the team. On their free time, they like to spend as much time as possible in the nature with family.",
          apiKey: "keys/example/exampleKey.key",
        },
      },
    };
  },
  methods: {
    handleAddErrorHandler() {
      this.errorHandler.push({
        config: {
          dedupe_key: "test",
          event_action: "trigger",
          images:
            "https://www.rundeck.com/hubfs/rundeck-app-assets/rundeck-by-pagerduty-icon.png",
          payload_severity: "info",
          payload_source: "${job.name}",
          payload_summary:
            "${job.name} [${job.project}] run by ${job.username} (type: ${job.executionType}) ",
          service_key: "keys/pagerduty-integration-key",
        },
        nodeStep: false,
        type: "pd-sent-event-step",
      });
    },
    handleAddElement() {
      this.logFilters.push({
        name: "quiet-output",
        title: "Quiet Output",
        description:
          "Quiets all output which does or does not match a certain pattern by changing its log level.",
        providerMetadata: { faicon: "volume-off" },
      });
    },
  },
});
</script>

<style lang="scss">
.plugin-config-section {
  border-bottom: 1px solid var(--p-accordion-panel-border-color);
  padding-bottom: var(--sizes-2);

  &:last-child {
    border-bottom: none;
    padding-bottom: 0;
    margin-top: var(--sizes-2);
  }
}

.configpair {
  > span:only-child {
    align-items: start;
    display: flex;
  }

  span[title] {
    color: var(--colors-gray-600);
    flex: 0 0 100px;
  }

  .copiable-text {
    color: var(--colors-gray-800);

    &:hover{
      color: var(--colors-gray-800);
    }
  }
}

.configprop {
  display: block;
  margin-bottom: var(--sizes-2);

  &:empty {
    display: none;
  }

  + .col-sm-12 {
    display: none;
  }
}
</style>