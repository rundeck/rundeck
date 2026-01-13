<template>
  <Card class="stepCard">
    <template #header>
      <StepCardHeader
        :plugin-details="pluginDetails"
        :config="config"
      />
    </template>
    <template #content>
      <StepCardContent
        :config="config"
        :service-name="computedServiceName"
        v-model:log-filters="logFiltersModel"
        v-model:error-handler="errorHandlerModel"
        :error-handler-plugin-info="errorHandlerPluginInfo"
        :error-handler-config="errorHandlerConfig"
        :error-handler-service-name="errorHandlerServiceName"
        :error-handler-provider="errorHandlerProvider"
        @add-log-filter="handleAddLogFilter"
        @add-error-handler="handleAddErrorHandler"
      />
    </template>
  </Card>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import Card from "primevue/card";
import StepCardHeader from "@/library/components/primeVue/StepCards/StepCardHeader.vue";
import StepCardContent from "@/library/components/primeVue/StepCards/StepCardContent.vue";

export default defineComponent({
  name: "StepCard",
  components: {
    Card,
    StepCardHeader,
    StepCardContent,
  },
  props: {
    pluginDetails: {
      type: Object,
      required: true,
    },
    config: {
      type: Object,
      required: true,
    },
    serviceName: {
      type: String,
      default: "WorkflowStep",
    },
    logFilters: {
      type: Array,
      default: () => [],
    },
    errorHandler: {
      type: Array,
      default: () => [],
    },
    errorHandlerPluginInfo: {
      type: Object,
      default: () => ({ title: "Command" }),
    },
    errorHandlerConfig: {
      type: Object,
      default: () => ({ adhocRemoteString: "echo error happened" }),
    },
    errorHandlerServiceName: {
      type: String,
      default: "WorkflowNodeStep",
    },
    errorHandlerProvider: {
      type: String,
      default: "exec-command",
    },
  },
  emits: ["update:logFilters", "update:errorHandler", "add-log-filter", "add-error-handler"],
  computed: {
    computedServiceName() {
      return this.serviceName || (this.config.nodeStep ? "WorkflowNodeStep" : "WorkflowStep");
    },
    logFiltersModel: {
      get() {
        return this.logFilters;
      },
      set(value) {
        this.$emit("update:logFilters", value);
      },
    },
    errorHandlerModel: {
      get() {
        return this.errorHandler;
      },
      set(value) {
        this.$emit("update:errorHandler", value);
      },
    },
  },
  methods: {
    handleAddLogFilter() {
      this.$emit("add-log-filter");
    },
    handleAddErrorHandler() {
      this.$emit("add-error-handler");
    },
  },
});
</script>

<style lang="scss" scoped>
.stepCard {
  box-shadow: none;
  overflow: hidden;
  border-radius: var(--radii-md);
  border: 1px solid var(--colors-gray-300);

  p,
  a,
  span:not(.glyphicon, .fa, .pi) {
    font-family: Inter, var(--fonts-body) !important;
  }

  :deep(.p-card-body) {
    padding: var(--sizes-4);
  }

}
</style>
