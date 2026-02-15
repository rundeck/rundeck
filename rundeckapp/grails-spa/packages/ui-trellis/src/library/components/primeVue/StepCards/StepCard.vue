<template>
  <BaseStepCard
    :plugin-details="pluginDetails"
    :config="config"
    :service-name="serviceName"
    :show-toggle="showToggle"
    :show-as-node-step="computedServiceName === 'WorkflowNodeStep'"
    :initially-expanded="initiallyExpanded"
    :disabled="disabled"
    @delete="handleDelete"
    @duplicate="handleDuplicate"
    @edit="handleEdit"
  >
    <template #content>
      <StepCardContent
        :config="config"
        :service-name="computedServiceName"
        :element-id="config.id || ''"
        :log-filters="logFilters"
        :error-handler="errorHandler"
        :error-handler-config="computedErrorHandlerConfig"
        :error-handler-service-name="computedErrorHandlerServiceName"
        :error-handler-provider="computedErrorHandlerProvider"
        :disabled="disabled"
        @add-log-filter="handleAddLogFilter"
        @add-error-handler="handleAddErrorHandler"
        @edit-log-filter="handleEditLogFilter"
        @edit-error-handler="handleEditErrorHandler"
        @remove-error-handler="handleRemoveErrorHandler"
        @update:log-filters="$emit('update:logFilters', $event)"
      />
    </template>
  </BaseStepCard>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import BaseStepCard from "@/library/components/primeVue/StepCards/BaseStepCard.vue";
import StepCardContent from "@/library/components/primeVue/StepCards/StepCardContent.vue";

export default defineComponent({
  name: "StepCard",
  components: {
    BaseStepCard,
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
    showToggle: {
      type: Boolean,
      default: false,
    },
    initiallyExpanded: {
      type: Boolean,
      default: true,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
  },
  emits: ["update:logFilters", "add-log-filter", "add-error-handler", "edit-log-filter", "edit-error-handler", "remove-error-handler", "delete", "duplicate", "edit"],
  computed: {
    computedServiceName() {
      return this.serviceName || (this.config.nodeStep ? "WorkflowNodeStep" : "WorkflowStep");
    },
    errorHandlerData() {
      return this.errorHandler && this.errorHandler.length > 0 ? this.errorHandler[0] : null;
    },
    computedErrorHandlerConfig() {
      return this.errorHandlerData?.config || {};
    },
    computedErrorHandlerServiceName() {
      return this.errorHandlerData?.nodeStep ? "WorkflowNodeStep" : "WorkflowStep";
    },
    computedErrorHandlerProvider() {
      return this.errorHandlerData?.type || "";
    },
  },
  methods: {
    handleAddLogFilter(elementId: string) {
      this.$emit("add-log-filter", elementId);
    },
    handleAddErrorHandler(elementId: string) {
      this.$emit("add-error-handler", elementId);
    },
    handleDelete() {
      this.$emit("delete");
    },
    handleDuplicate() {
      this.$emit("duplicate");
    },
    handleEdit() {
      this.$emit("edit");
    },
    handleEditLogFilter(data: { filter: any; index: number }) {
      this.$emit("edit-log-filter", data);
    },
    handleEditErrorHandler(elementId: string) {
      this.$emit("edit-error-handler", elementId);
    },
    handleRemoveErrorHandler(elementId: string) {
      this.$emit("remove-error-handler", elementId);
    },
  },
});
</script>
