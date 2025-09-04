<template>
  <Card class="stepCard">
    <template #header>
      <StepCardHeader :plugin-details="pluginDetails" :config="pdStep" />
    </template>
    <template #content>
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
        tooltip="Only linux nodes will execute the following steps"
        v-model="logFilters"
        @addElement="handleAddElement"
      />
      <ConfigSection
        title="Error Handler"
        tooltip="Only linux nodes will execute the following steps"
        v-model="errorHandler"
        @addElement="handleAddElement"
        hideWhenSingle
      >
        <template #header> test </template>
      </ConfigSection>
    </template>
  </Card>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import Card from "primevue/card";
import StepCardHeader from "@/library/components/primeVue/StepCards/StepCardHeader.vue";
import ConfigSection from "@/library/components/primeVue/StepCards/ConfigSection.vue";
import PluginConfig from "@/library/components/plugins/pluginConfig.vue";

export default defineComponent({
  name: "StepCard",
  components: {
    PluginConfig,
    Card,
    StepCardHeader,
    ConfigSection,
  },
  props: {},
  data() {
    return {
      logFilters: [],
      errorHandler: [],
      pluginDetails: {
        title: "PagerDuty / User / Create",
        description: "Create a user",
        iconUrl: "public/images/icon-pagerduty.png",
      },
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
    handleMoreActions() {
      // Handle more actions logic
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
.stepCard {
  box-shadow: none;
  overflow: hidden;
  border-radius: var(--radii-md);
  border: 1px solid var(--colors-gray-300);

  p,
  a,
  span:not(.glyphicon, .fa) {
    font-family: Inter, var(--fonts-body) !important;
  }

  .p-card-body {
    padding: var(--sizes-4);
  }

  .plugin-config-section {
    border-bottom: 1px solid var(--p-accordion-panel-border-color);
    padding-bottom: var(--sizes-2);
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
}
</style>
