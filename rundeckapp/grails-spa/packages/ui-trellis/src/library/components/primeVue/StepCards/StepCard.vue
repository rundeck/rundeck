<template>
  <Card class="stepCard">
    <template #header>
      <StepCardHeader
        :plugin-details="pluginDetails"
        :config="config"
        @delete="handleDelete"
        @duplicate="handleDuplicate"
        @edit="handleEdit"
      />
    </template>
    <template #content>
      <StepCardContent
        :config="config"
        :service-name="computedServiceName"
        :element-id="config.id || ''"
        v-model:log-filters="logFiltersModel"
        :error-handler="errorHandler"
        :error-handler-config="computedErrorHandlerConfig"
        :error-handler-service-name="computedErrorHandlerServiceName"
        :error-handler-provider="computedErrorHandlerProvider"
        @add-log-filter="handleAddLogFilter"
        @add-error-handler="handleAddErrorHandler"
        @edit-log-filter="handleEditLogFilter"
        @edit-error-handler="handleEditErrorHandler"
        @remove-error-handler="handleRemoveErrorHandler"
      />
    </template>
  </Card>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import Card from "primevue/card";
import StepCardHeader from "@/library/components/primeVue/StepCards/StepCardHeader.vue";
import StepCardContent from "@/library/components/primeVue/StepCards/StepCardContent.vue";
import { getRundeckContext } from "@/library";

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
  },
  emits: ["update:logFilters", "add-log-filter", "add-error-handler", "edit-log-filter", "edit-error-handler", "remove-error-handler", "delete", "duplicate", "edit"],
  computed: {
    computedServiceName() {
      return this.serviceName || (this.config.nodeStep ? "WorkflowNodeStep" : "WorkflowStep");
    },
    eventBus() {
      return getRundeckContext()?.eventBus;
    },
    errorHandlerData() {
      // Extract error handler from array (it's passed as [element.errorhandler])
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
    logFiltersModel: {
      get() {
        return this.logFilters;
      },
      set(value) {
        this.$emit("update:logFilters", value);
      },
    },
  },
  methods: {
    handleAddLogFilter() {
      // Emit component event - parent will call addLogFilterForIndex
      this.$emit("add-log-filter");
    },
    handleAddErrorHandler() {
      // Emit component event - parent template will call handler with index
      this.$emit("add-error-handler");
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
      // Forward the edit-log-filter event to parent
      this.$emit("edit-log-filter", data);
    },
    handleEditErrorHandler() {
      // Forward the edit-error-handler event to parent - WorkflowSteps will handle it
      this.$emit("edit-error-handler");
    },
    handleRemoveErrorHandler() {
      // Forward the remove-error-handler event to parent - WorkflowSteps will handle it
      this.$emit("remove-error-handler");
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
  span:not(.glyphicon, .fa, .pi) {
    font-family: Inter, var(--fonts-body) !important;
  }

  .p-card-body {
    padding: var(--sizes-4);
  }
}
</style>
