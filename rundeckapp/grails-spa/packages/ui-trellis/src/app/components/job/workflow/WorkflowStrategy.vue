<template>
  <div class="form-inline" v-if="loaded">
    <div
      class="form-group ${hasErrors(bean: workflow, field: 'strategy', 'has-error')}"
    >
      <label class="col-sm-12" title="Strategy for iteration">
        {{ $t("Workflow.property.strategy.label") }}:

        <select
          class="form-control"
          v-model="model.strategy"
          v-if="pluginProviders && pluginProviders.length > 0"
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
    </div>
  </div>

  <PluginDetails
    :description="selectedPlugin.description"
    :description-css="'text-info'"
    v-if="selectedPlugin"
  />
  <plugin-config
    :key="selectedPlugin.name"
    v-model="editStrategyPlugin"
    :mode="'edit'"
    :service-name="'WorkflowStrategy'"
    :show-title="false"
    :show-description="false"
    :context-autocomplete="true"
    :validation="editValidation"
    scope="Instance"
    default-scope="Instance"
    groupCss=""
    v-if="selectedPlugin"
  ></plugin-config>
</template>
<script lang="ts">
import {
  createStrategyData,
  StrategyData,
} from "@/app/components/job/workflow/types/workflowTypes";
import PluginConfig from "@/library/components/plugins/pluginConfig.vue";
import PluginDetails from "@/library/components/plugins/PluginDetails.vue";
import { getPluginProvidersForService } from "@/library/modules/pluginService";
import PluginInfo from "@/library/components/plugins/PluginInfo.vue";
export default {
  name: "WorkflowStrategy",
  components: {
    PluginConfig,
    PluginDetails,
    PluginInfo,
  },
  emits: ["update:modelValue"],
  props: {
    modelValue: {
      type: Object,
      required: true,
      default: () => ({}) as StrategyData,
    },
  },
  data() {
    return {
      model: {} as StrategyData,
      pluginProviders: [],
      pluginLabels: [],
      loaded: false,
      editStrategyPlugin: { type: "", config: {} },
      editValidation: null,
    };
  },
  watch: {
    model: {
      handler() {
        this.editStrategyPlugin.type = this.model.strategy;
        this.editStrategyPlugin.config = this.model.strategyConfig || {};
        this.$emit("update:modelValue", this.model);
      },
      deep: true,
    },
    editStrategyPlugin: {
      handler() {
        this.model.strategy = this.editStrategyPlugin.type;
        this.model.strategyConfig = this.editStrategyPlugin.config || {};
      },
      deep: true,
    },
  },
  computed: {
    selectedPlugin() {
      if (!this.model.strategy) {
        return null;
      }
      return this.pluginProviders.find(
        (p: any) => p.name === this.model.strategy,
      );
    },
  },
  methods: {
    async getStrategyPlugins() {
      const response = await getPluginProvidersForService("WorkflowStrategy");
      if (response.service) {
        this.pluginProviders = response.descriptions;
        this.pluginLabels = response.labels;
      }
    },
  },
  async mounted() {
    this.model = createStrategyData(this.modelValue, (origVal: string) => {
      if (origVal === "step-first") {
        return "sequential";
      } else if (typeof origVal === "string" && origVal !== "") {
        return origVal;
      } else if (!origVal || origVal === "") {
        return "node-first";
      }
    });
    this.editStrategyPlugin.type = this.model.strategy;
    this.editStrategyPlugin.config = this.model.strategyConfig || {};

    await this.getStrategyPlugins();
    this.loaded = true;
  },
};
</script>
