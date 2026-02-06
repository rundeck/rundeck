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
          <Accordion v-if="steps && steps.length > 0" multiple expandIcon="pi pi-chevron-down" collapseIcon="pi pi-chevron-up">
            <AccordionPanel
              v-for="(step, index) in steps"
              :key="step.id || index"
              :value="String(index + 1)"
            >
              <AccordionHeader>
                <plugin-info
                  v-if="step.pluginDetails"
                  class="conditionalCard--step-description"
                  :detail="step.pluginDetails"
                  :show-description="false"
                  :show-title="false"
                  :show-extended="false"
                >
                  <template #titleprefix>
                    <span class="link-step-plugin" @click.stop="handleStepClick(step, index)">
                      {{ step.description || step.name || $t("Workflow.stepLabel") }}
                    </span>
                    <i class="pi pi-pencil"/>
                  </template>
                </plugin-info>
              </AccordionHeader>
              <AccordionContent v-if="step.content">
                <div v-html="step.content"></div>
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

interface ConditionalStep {
  id?: string;
  name?: string;
  description?: string;
  pluginDetails?: any;
  content?: string;
}

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
      type: Array as PropType<ConditionalStep[]>,
      default: () => [],
    },
  },
  emits: ["update:logFilters", "update:errorHandler", "add-log-filter", "add-error-handler", "delete", "duplicate", "edit", "step-click"],
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
    handleStepClick(step: ConditionalStep, index: number) {
      this.$emit("step-click", { step, index });
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
