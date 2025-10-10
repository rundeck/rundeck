<template>
  <div v-if="loaded" class="form-inline">
    <div class="form-group" :class="{ 'has-error': false }">
      <label class="col-sm-12" title="Strategy for iteration">
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
    </div>
  </div>

  <div v-if="selectedPlugin" :id="`strategyPlugin${selectedPlugin.name}`">
    <PluginDetails
      :description="selectedPlugin.description"
      :description-css="'text-info'"
    />
  </div>

  <plugin-config
    v-if="selectedPlugin"
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
</template>
<script lang="ts">
import PluginConfig from "@/library/components/plugins/pluginConfig.vue";
import PluginDetails from "@/library/components/plugins/PluginDetails.vue";
import PluginInfo from "@/library/components/plugins/PluginInfo.vue";
import { getPluginProvidersForService } from "@/library/modules/pluginService";
import { defineComponent } from "vue";

export default defineComponent({
  name: "WorkflowStrategy",
  components: {
    PluginConfig,
    PluginDetails,
    PluginInfo,
  },
  props: {
    modelValue: {
      type: Object,
      required: true,
      default: () => ({}) as PluginConfig,
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
