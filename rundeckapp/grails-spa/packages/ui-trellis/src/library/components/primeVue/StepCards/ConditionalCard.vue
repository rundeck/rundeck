<template>
  <Card class="conditionalCard" :class="{'complex': complex}">
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
      <Conditional :complex="complex" :condition-sets="conditionSets">
        <slot name="steps">
          <Accordion v-if="computedSteps && computedSteps.length > 0" multiple expandIcon="pi pi-chevron-down" collapseIcon="pi pi-chevron-up">
            <AccordionPanel
              v-for="(step, index) in computedSteps"
              :key="step.id || index"
              :value="String(index + 1)"
              :class="{ 'align-start': step.type === 'conditional.logic' }"
            >
              <AccordionHeader v-if="step.type === 'conditional.logic'" as-child>
                <div class="p-accordionheader nested">
                  <Conditional
                    :complex="((step.config?.conditionSets?.length) || 0) > 1"
                    :condition-sets="step.config?.conditionSets || []"
                  >
                    <Accordion v-if="(step.config?.commands?.length || 0) > 0" multiple expandIcon="pi pi-chevron-down" collapseIcon="pi pi-chevron-up">
                      <AccordionPanel
                        v-for="(nestedStep, nestedIndex) in (step.config?.commands || [])"
                        :key="nestedStep.id || nestedIndex"
                        :value="String(nestedIndex + 1)"
                      >
                        <AccordionHeader>
                          <plugin-info
                            v-if="getStepPluginDetails(nestedStep)"
                            class="conditionalCard--step-description"
                            :detail="getStepPluginDetails(nestedStep)"
                            :show-description="false"
                            :show-title="false"
                            :show-extended="false"
                          >
                            <template #titleprefix>
                              <span class="link-step-plugin" @click.stop="handleNestedStepClick(nestedStep, nestedIndex)">
                                {{ nestedStep.description || nestedStep.type || $t("Workflow.stepLabel") }}
                              </span>
                              <i class="pi pi-pencil"/>
                            </template>
                          </plugin-info>
                        </AccordionHeader>
                        <AccordionContent>
                          <StepCardContent
                            :config="nestedStep"
                            :service-name="computedServiceName"
                            :element-id="nestedStep.id || ''"
                            :log-filters="nestedStep.filters || []"
                            :error-handler="nestedStep.errorhandler ? [nestedStep.errorhandler] : []"
                            :error-handler-config="getStepErrorHandlerConfig(nestedStep)"
                            :error-handler-service-name="getStepErrorHandlerServiceName(nestedStep)"
                            :error-handler-provider="getStepErrorHandlerProvider(nestedStep)"
                            @add-log-filter="$emit('add-log-filter')"
                            @add-error-handler="$emit('add-error-handler')"
                            @edit-log-filter="$emit('edit-log-filter', $event)"
                            @edit-error-handler="$emit('edit-error-handler')"
                            @remove-error-handler="$emit('remove-error-handler')"
                            @update:log-filters="$emit('update:logFilters', $event)"
                          />
                        </AccordionContent>
                      </AccordionPanel>
                    </Accordion>
                  </Conditional>
                </div>
              </AccordionHeader>
              <AccordionHeader v-else>
                <plugin-info
                  v-if="getStepPluginDetails(step)"
                  class="conditionalCard--step-description"
                  :detail="getStepPluginDetails(step)"
                  :show-description="false"
                  :show-title="false"
                  :show-extended="false"
                >
                  <template #titleprefix>
                    <span class="link-step-plugin" @click.stop="handleStepClick(step, index)">
                      {{ step.description || step.type || $t("Workflow.stepLabel") }}
                    </span>
                    <i class="pi pi-pencil"/>
                  </template>
                </plugin-info>
              </AccordionHeader>
              <AccordionContent v-if="step.type !== 'conditional.logic'">
                <StepCardContent
                  :config="step"
                  :service-name="computedServiceName"
                  :element-id="step.id || ''"
                  :log-filters="step.filters || []"
                  :error-handler="step.errorhandler ? [step.errorhandler] : []"
                  :error-handler-config="getStepErrorHandlerConfig(step)"
                  :error-handler-service-name="getStepErrorHandlerServiceName(step)"
                  :error-handler-provider="getStepErrorHandlerProvider(step)"
                  @add-log-filter="$emit('add-log-filter')"
                  @add-error-handler="$emit('add-error-handler')"
                  @edit-log-filter="$emit('edit-log-filter', $event)"
                  @edit-error-handler="$emit('edit-error-handler')"
                  @remove-error-handler="$emit('remove-error-handler')"
                  @update:log-filters="$emit('update:logFilters', $event)"
                />
              </AccordionContent>
            </AccordionPanel>
          </Accordion>
        </slot>
      </Conditional>
    </template>
  </Card>
</template>

<script lang="ts">
import { defineComponent, type PropType } from "vue";
import Card from "primevue/card";
import Accordion from "primevue/accordion";
import AccordionPanel from "primevue/accordionpanel";
import AccordionHeader from "primevue/accordionheader";
import AccordionContent from "primevue/accordioncontent";
import Tag from "primevue/tag";
import Menu from "primevue/menu";
import Button from "primevue/button";
import Conditional from "./Conditional.vue";
import PluginInfo from "../../plugins/PluginInfo.vue";
import PluginConfig from "../../plugins/pluginConfig.vue";
import PtButton from "../PtButton/PtButton.vue";
import LogFilters from "@/app/components/job/workflow/LogFilters.vue";
import ErrorHandlerStep from "@/app/components/job/workflow/ErrorHandlerStep.vue";
import StepCardHeader from "@/library/components/primeVue/StepCards/StepCardHeader.vue";
import StepCardContent from "@/library/components/primeVue/StepCards/StepCardContent.vue";
import { getRundeckContext } from "@/library";
import { getPluginDetailsForStep } from "@/app/components/job/workflow/stepEditorUtils";
import type { EditStepData } from "@/app/components/job/workflow/types/workflowTypes";


import "../Tag/tag.scss";
import "../Tooltip/tooltip.scss";

export default defineComponent({
  name: "ConditionalCard",
  components: {
    StepCardContent,
    StepCardHeader,
    ErrorHandlerStep,
    LogFilters,
    PluginConfig,
    PtButton,
    PluginInfo,
    Conditional,
    Accordion,
    AccordionPanel,
    AccordionHeader,
    AccordionContent,
    Button,
    Card,
    Menu,
    Tag,
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
    steps: {
      type: Array as PropType<EditStepData[]>,
      default: () => [],
    },
  },
  emits: ["update:logFilters", "update:errorHandler", "add-log-filter", "add-error-handler", "edit-log-filter", "edit-error-handler", "remove-error-handler", "delete", "duplicate", "edit", "step-click"],
  computed: {
    computedServiceName() {
      return this.serviceName || (this.config.nodeStep ? "WorkflowNodeStep" : "WorkflowStep");
    },
    eventBus() {
      return getRundeckContext()?.eventBus;
    },
    conditionSets() {
      return this.config?.config?.conditionSets || [];
    },
    computedSteps(): EditStepData[] {
      const commands = this.config?.config?.commands || [];
      if (commands.length === 0) {
        return this.steps || [];
      }
      return commands;
    },
    complex() {
      return this.conditionSets.length > 1;
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
      // Emit eventBus event like WorkflowSteps.addLogFilterForIndex does
      const elementId = this.config.id || "";
      if (this.eventBus && elementId) {
        this.eventBus.emit("step-action:add-logfilter:" + elementId);
      }
      // Emit component event - parent template will call handler with element.id
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
    getStepPluginDetails(step: EditStepData | { pluginDetails?: any }) {
      if ((step as any).pluginDetails) {
        return (step as any).pluginDetails;
      }
      return getPluginDetailsForStep(step as EditStepData);
    },
    getStepErrorHandlerConfig(step: EditStepData) {
      const eh = step.errorhandler;
      return eh?.config || {};
    },
    getStepErrorHandlerServiceName(step: EditStepData) {
      const eh = step.errorhandler;
      return eh?.nodeStep ? "WorkflowNodeStep" : "WorkflowStep";
    },
    getStepErrorHandlerProvider(step: EditStepData) {
      const eh = step.errorhandler;
      return eh?.type || "";
    },
    handleStepClick(step: EditStepData, index: number) {
      this.$emit("step-click", { step, index });
    },
    handleNestedStepClick(step: EditStepData, index: number) {
      this.$emit("step-click", { step, index, parentStepId: this.config.id });
    },
  },
});
</script>

<style lang="scss">
.conditionalCard {
  box-shadow: none;
  overflow: hidden;
  border-radius: var(--radii-md);
  border: 1px solid var(--colors-gray-300-original);

  p,
  a,
  span:not(.glyphicon, .fa, .pi) {
    font-family: Inter, var(--fonts-body) !important;
  }

  &--header {
    background-color: var(--colors-secondaryBackgroundOnLight);
    border-bottom: 2px solid var(--colors-gray-300-original);
    display: flex;
    justify-content: space-between;
    padding: var(--sizes-4);

    &-description {
      align-items: center;
      display: flex;
      flex-direction: row;
      gap: 4px;
      margin-top: var(--sizes-3);

      p {
        font-weight: var(--fontWeights-medium);
        font-size: var(--fontSizes-md);
        margin: 0;
      }

      .pi {
        margin-left: 0;
      }
    }

    &-buttons {
      align-items: flex-start;
      display: flex;
      gap: var(--sizes-2);
    }
  }

  &--step {
    &-description {
      align-items: center;
      display: flex;
      flex: 1 1 auto;
      gap: var(--sizes-2);
    }
  }

  .p-card-body {
    padding: var(--sizes-4);
  }

  .p-accordioncontent-content {
    background: var(--colors-gray-50);
  }
}

.p-menu-list {
  margin: 0;
  padding: var(--space-1) var(--space-1);
}
</style>
