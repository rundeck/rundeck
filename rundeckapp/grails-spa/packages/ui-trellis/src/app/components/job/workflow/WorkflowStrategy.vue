<template>
  <div :class="conditionalEnabled ? 'conditional-enabled' : ''">
    <div v-if="loaded">
      <h3 v-if="conditionalEnabled" class="text-heading--md">
        {{ $t("Workflow.property.strategy.label") }}
      </h3>
      <p v-if="conditionalEnabled" class="text-body--lg strategy-description">
        {{ $t("Workflow.property.strategy.description") }}
        <a href="#" class="text-body--primary">{{ $t("Workflow.property.strategy.learnMore") }}</a>
      </p>
      <div :class="conditionalEnabled ? 'col-xs-12' : 'form-inline'">
        <div class="form-group" :class="{ 'has-error': false }">
          <label v-if="!conditionalEnabled" :class="conditionalEnabled ? '' : 'col-sm-12'" title="Strategy for iteration">
            {{ $t("Workflow.property.strategy.label") }}:

            <select
              v-if="pluginProviders && pluginProviders.length > 0"
              v-model="model.type"
              class="form-control"
              name="workflow.strategy"
            >
              <option
                v-for="plugin in pluginProviders"
                :key="plugin.name"
                :value="plugin.name"
              >
                {{ plugin.title }}
              </option>
            </select>
          </label>
          <div
            v-else-if="conditionalEnabled && pluginProviders && pluginProviders.length > 0"
            :class="['strategy-select-wrapper', { 'strategy-select-with-config': conditionalEnabled && selectedPlugin && selectedPlugin.name === 'ruleset' }]"
          >
            <PtSelect
              v-model="model.type"
              :options="strategyOptions"
              :placeholder="$t('Workflow.property.strategy.selectPlaceholder')"
              name="workflow.strategy"
              option-label="title"
              option-value="name"
            >
              <template #option="slotProps">
                <div class="strategy-option">
                  <div class="strategy-option-title">{{ slotProps.option.title }}</div>
                  <div v-if="slotProps.option.description" class="strategy-option-description">
                    {{ slotProps.option.description }}
                  </div>
                </div>
              </template>
            </PtSelect>
          </div>
        </div>
      </div>
    </div>

    <div v-if="selectedPlugin && !conditionalEnabled" :id="`strategyPlugin${selectedPlugin.name}`">
      <PluginDetails
        :description="selectedPlugin.description"
        :description-css="'text-info'"
      />
    </div>

    <plugin-config
      v-if="conditionalEnabled ? (selectedPlugin && selectedPlugin.name === 'ruleset') : selectedPlugin"
      :key="selectedPlugin.name"
      v-model="editStrategyPlugin"
      :mode="mode"
      :service-name="'WorkflowStrategy'"
      :show-title="false"
      :show-description="false"
      :context-autocomplete="true"
      :validation="editValidation"
      scope="Instance"
      default-scope="Instance"
      group-css=""
    ></plugin-config>
  </div>
</template>
<script lang="ts">
import PluginConfig from "@/library/components/plugins/pluginConfig.vue";
import PluginDetails from "@/library/components/plugins/PluginDetails.vue";
import PluginInfo from "@/library/components/plugins/PluginInfo.vue";
import PtSelect from "@/library/components/primeVue/PtSelect/PtSelect.vue";
import { getPluginProvidersForService } from "@/library/modules/pluginService";
import { defineComponent } from "vue";

export default defineComponent({
  name: "WorkflowStrategy",
  components: {
    PluginConfig,
    PluginDetails,
    PluginInfo,
    PtSelect,
  },
  props: {
    modelValue: {
      type: Object,
      required: true,
      default: () => ({}) as PluginConfig,
    },
    conditionalEnabled: {
      type: Boolean,
      default: false,
    },
  },
  emits: ["update:modelValue"],
  data() {
    return {
      model: {} as PluginConfig,
      mode: "create",
      pluginProviders: [],
      pluginLabels: [],
      loaded: false,
      editStrategyPlugin: { type: "", config: {} } as PluginConfig,
      editValidation: null,
    };
  },
  computed: {
    selectedPlugin() {
      if (!this.model.type) {
        return null;
      }
      return this.pluginProviders.find((p: any) => p.name === this.model.type);
    },
    strategyOptions() {
      return this.pluginProviders || [];
    },
  },
  watch: {
    model: {
      handler(newVal, oldVal) {
        if (newVal.type !== oldVal.type) {
          this.mode = "create";
        }
        this.editStrategyPlugin.type = this.model.type;
        this.editStrategyPlugin.config = this.model.config || {};
        this.$emit("update:modelValue", this.model);
      },
      deep: true,
    },
    editStrategyPlugin: {
      handler() {
        this.model.type = this.editStrategyPlugin.type;
        this.model.config = this.editStrategyPlugin.config || {};
      },
      deep: true,
    },
  },
  async mounted() {
    this.model = {
      type: this.cleanStrategy(this.modelValue.type),
      config: this.modelValue.config,
    };
    this.editStrategyPlugin.type = this.model.type;
    this.editStrategyPlugin.config = this.model.config || {};
    this.mode = this.model.type ? "edit" : "create";
    await this.getStrategyPlugins();
    this.loaded = true;
  },
  methods: {
    cleanStrategy(strategy: string) {
      if (strategy === "step-first") {
        return "sequential";
      } else if (typeof strategy === "string" && strategy !== "") {
        return strategy;
      } else if (!strategy || strategy === "") {
        return "node-first";
      }
      return strategy;
    },
    async getStrategyPlugins() {
      const response = await getPluginProvidersForService("WorkflowStrategy");
      if (response.service) {
        this.pluginProviders = response.descriptions;
        this.pluginLabels = response.labels;
      }
    },
  },
});
</script>

<style scoped lang="scss">
.conditional-enabled {
  margin-top:35px;
  margin-bottom: 35px;
  overflow: visible;

  &:after {
    content: " ";
    clear: both;
    display: block;
  }

  h3 {
    margin: 0 0 16px 0;
  }

  .strategy-description {
    margin-bottom: 12px;
  }

  .strategy-select-wrapper {
    max-width: 700px;
  }

  .strategy-select-with-config {
    max-width: 700px;
    margin-bottom: 36px;
  }
}
</style>
<style>
.strategy-option {
  padding: 0;
}

.strategy-option-title {
  font-size: 14px;
  font-weight: 700 !important;
  line-height: 1.5;
}

.strategy-option-description {
  font-size: 12px;
  line-height: 1.4;
  margin-top: 4px;
}
</style>