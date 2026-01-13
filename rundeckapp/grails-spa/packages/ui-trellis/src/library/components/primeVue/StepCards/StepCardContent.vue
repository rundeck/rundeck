<template>
  <div class="step-card-content">
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
    <ConfigSection
      :title="$t('Workflow.logFilters')"
      :tooltip="$t('Workflow.logFiltersTooltip')"
      v-model="logFiltersModel"
      @addElement="handleAddElement"
      @editElement="handleEditLogFilter"
    />
    <ConfigSection
      :title="$t('Workflow.addErrorHandler')"
      :tooltip="$t('Workflow.errorHandlerDescription')"
      v-model="errorHandlerModel"
      @addElement="handleAddErrorHandler"
      hideWhenSingle
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
      <template #extra v-if="errorHandler.length >= 1">
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
import { getRundeckContext } from "@/library";
import { PluginConfig as PluginConfigType } from "@/library/interfaces/PluginConfig";
import { getPluginProvidersForService } from "@/library/modules/pluginService";

export default defineComponent({
  name: "StepCardContent",
  components: {
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
  },
  emits: ["add-log-filter", "add-error-handler", "edit-log-filter", "update:logFilters", "update:errorHandler"],
  data() {
    return {
      logFilterPluginProviders: [] as any[],
      globalEventBus: getRundeckContext()?.eventBus,
    };
  },
  computed: {
    errorHandlerData() {
      return this.errorHandler && this.errorHandler.length > 0 ? this.errorHandler[0] : null;
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
    handleAddErrorHandler() {
      this.$emit("add-error-handler");
    },
    handleAddElement() {
      // Emit to parent - parent will handle opening modal
      this.$emit("add-log-filter");
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