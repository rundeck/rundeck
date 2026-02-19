<template>
  <div class="step-card-content">
    <div v-if="config.jobref" class="plugin-config-section" data-testid="step-card-content-jobref-section">
      <span v-for="prop in jobRefProps" :key="prop.name" class="configprop">
        <plugin-prop-view
          :prop="prop"
          :value="jobRefConfig[prop.name]"
          :allow-copy="true"
        />
      </span>
    </div>
    <div v-else data-testid="step-card-content-plugin-config">
      <plugin-config
        class="plugin-config-section"
        :serviceName="serviceName"
        :provider="config.type"
        :config="config.config"
        :readOnly="true"
        :showTitle="false"
        :showIcon="false"
        :showDescription="false"
        mode="show"
        allowCopy
      />
    </div>
    <ConfigSection
      v-if="!hideConfigSection && !config.jobref"
      data-testid="step-card-content-log-filters"
      :title="$t('Workflow.logFilters')"
      :tooltip="$t('Workflow.logFiltersTooltip')"
      :model-value="logFilters"
      :disabled="disabled"
      @update:model-value="$emit('update:logFilters', $event)"
      @addElement="handleAddElement"
      @editElement="handleEditLogFilter"
    />
    <ConfigSection
      v-if="!hideConfigSection"
      data-testid="step-card-content-error-handler"
      :title="$t('Workflow.addErrorHandler')"
      :tooltip="$t('Workflow.errorHandlerDescription')"
      :model-value="errorHandler"
      :disabled="disabled"
      @addElement="handleAddErrorHandler"
      @editElement="handleEditErrorHandler"
      @removeElement="handleRemoveErrorHandler"
      hideWhenSingle
      :hideIcon="true"
      class="error-handler"
    >
      <template #header v-if="errorHandler.length >= 1">
        <plugin-info
          :detail="errorHandlerData"
          :show-description="false"
          :show-extended="false"
          :show-icon="false"
          titleCss="link-step-plugin"
        />
      </template>
      <template #content v-if="errorHandler.length >= 1">
        <plugin-config
          class="plugin-config-section"
          :serviceName="errorHandlerServiceName"
          :provider="errorHandlerProvider"
          :config="errorHandlerConfig"
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
import PluginPropView from "@/library/components/plugins/pluginPropView.vue";
import { getRundeckContext } from "@/library";

export default defineComponent({
  name: "StepCardContent",
  components: {
    PluginPropView,
    PluginInfo,
    PluginConfig,
    ConfigSection,
  },
  props: {
    config: {
      type: Object,
      required: true,
    },
    serviceName: {
      type: String,
      default: "WorkflowStep",
    },
    elementId: {
      type: String,
      default: "",
    },
    logFilters: {
      type: Array,
      default: () => [],
    },
    errorHandler: {
      type: Array,
      default: () => [],
    },
    errorHandlerConfig: {
      type: Object,
      default: () => ({}),
    },
    errorHandlerServiceName: {
      type: String,
      default: "WorkflowNodeStep",
    },
    errorHandlerProvider: {
      type: String,
      default: "",
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    hideConfigSection: {
      type: Boolean,
      default: false
    }
  },
  emits: [
    "add-log-filter",
    "add-error-handler",
    "edit-log-filter",
    "edit-error-handler",
    "remove-error-handler",
    "update:logFilters",
  ],
  data() {
    return {
      globalEventBus: getRundeckContext()?.eventBus,
    };
  },
  computed: {
    errorHandlerData() {
      return this.errorHandler && this.errorHandler.length > 0
        ? this.errorHandler[0]
        : null;
    },
    jobRefFullName(): string {
      if (!this.config.jobref) return "";
      if (this.config.jobref.name) {
        return (
          (this.config.jobref.group ? this.config.jobref.group + "/" : "") +
          this.config.jobref.name
        );
      }
      return this.config.jobref.uuid || "";
    },
    jobRefProps(): any[] {
      if (!this.config.jobref) return [];
      const props = [];

      props.push({
        name: "job",
        title: this.$t("job.label"),
        desc: "Job",
        type: "String",
      });

      if (this.config.jobref.project) {
        props.push({
          name: "project",
          title: this.$t("Project"),
          desc: "Project",
          type: "String",
        });
      }

      if (this.config.jobref.args) {
        props.push({
          name: "args",
          title: this.$t("Workflow.Step.argString.label"),
          desc: "Arguments",
          type: "String",
        });
      }

      if (this.config.jobref.nodeStep) {
        props.push({
          name: "nodeStep",
          title: this.$t("JobExec.nodeStep.true.label"),
          desc: "Node Step",
          type: "Boolean",
        });
      }

      if (this.config.jobref.nodefilters?.filter) {
        props.push({
          name: "nodefilters",
          title: this.$t("node.filter"),
          desc: "Node Filter",
          type: "String",
        });
      }

      return props;
    },
    jobRefConfig(): any {
      if (!this.config.jobref) return {};
      return {
        job: this.jobRefFullName,
        project: this.config.jobref.project,
        args: this.config.jobref.args,
        nodeStep: this.config.jobref.nodeStep ? "true" : "false",
        nodefilters: this.config.jobref.nodefilters?.filter,
      };
    },
  },
  methods: {
    handleAddErrorHandler() {
      this.$emit("add-error-handler", this.elementId);
    },
    handleAddElement() {
      // Emit to parent - parent will handle opening modal
      this.$emit("add-log-filter", this.elementId);
    },
    handleEditLogFilter(filter: any, index: number) {
      // Find the index in the logFilters array if not provided
      let filterIndex = index;
      if (filterIndex === undefined || filterIndex === null) {
        filterIndex = this.logFilters.findIndex((f: any) => {
          // Match by type or by object reference
          return (
            f === filter || (f.type && filter.type && f.type === filter.type)
          );
        });
      }
      // Emit with both filter and index for parent to use
      this.$emit("edit-log-filter", { filter, index: filterIndex });
    },
    handleEditErrorHandler() {
      // Guard: only emit if error handler exists
      if (!this.errorHandlerData) {
        return;
      }
      this.$emit("edit-error-handler", this.elementId);
    },
    handleRemoveErrorHandler() {
      this.$emit("remove-error-handler", this.elementId);
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

    &:hover {
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
