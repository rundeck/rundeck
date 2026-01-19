<template>
  <div
    v-if="!conditionalEnabled"
    class="log-filters-container"
    :class="{ inline: mode === 'inline' }"
    data-testid="log-filters-container"
  >
    <template v-if="showIfEmpty || model.length > 0">{{ title }}</template>
    <div v-if="model.length > 0 && mode === 'inline' && !conditionalEnabled" class="add-gap" :id="id">
      <template v-for="(entry, i) in model">
        <LogFilterButton
          v-if="findProvider(entry.type)"
          :key="`logFilter${i}`"
          :plugin-description="findProvider(entry.type)"
          @edit-filter="editFilterByIndex(i)"
          @remove-filter="removeFilterIndex(i)"
        ></LogFilterButton>
      </template>
    </div>
  </div>
  <div
    v-if="model.length > 0 && mode !== 'inline' && !conditionalEnabled"
    class="log-filters-container"
    data-testid="log-filters-button-container"
    :id="id"
  >
    <template v-for="(entry, i) in model">
      <LogFilterButton
        v-if="findProvider(entry.type)"
        :key="`logFilter${i}`"
        :plugin-description="findProvider(entry.type)"
        @edit-filter="editFilterByIndex(i)"
        @remove-filter="removeFilterIndex(i)"
      ></LogFilterButton>
    </template>
  </div>
  <log-filter-controls
    v-if="filtersEb"
    v-model="editModel"
    :title="title"
    :subtitle="subtitle"
    :event-bus="filtersEb"
    :show-button="conditionalEnabled ? false : (showIfEmpty || model.length > 0)"
    @update:model-value="saveEditFilter"
    @cancel="clearEdit"
  />
</template>

<script lang="ts">
import LogFilterButton from "@/app/components/job/workflow/LogFilterButton.vue";
import { getRundeckContext } from "@/library";
import { PluginConfig } from "@/library/interfaces/PluginConfig";
import { getPluginProvidersForService } from "@/library/modules/pluginService";
import { ServiceType } from "@/library/stores/Plugins";
import LogFilterControls from "@/app/components/job/workflow/LogFilterControls.vue";
import { cloneDeep } from "lodash";
import mitt from "mitt";
import { defineComponent } from "vue";
const eventBus = getRundeckContext().eventBus;
export default defineComponent({
  name: "LogFilters",
  components: { LogFilterButton, LogFilterControls },
  props: {
    modelValue: {
      type: Array,
      required: true,
      default: () => ({}) as PluginConfig[],
    },
    showIfEmpty: {
      type: Boolean,
      required: false,
      default: false,
    },
    title: {
      type: String,
      required: true,
    },
    subtitle: {
      type: String,
      required: true,
    },
    addEvent: {
      type: String,
      required: false,
    },
    editEvent: {
      type: String,
      required: false,
    },
    mode: {
      type: String,
      required: false,
      default: "block",
    },
    id: {
      type: String,
      default: "logFilters",
    },
    conditionalEnabled: {
      type: Boolean,
      default: false,
    },
  },
  emits: ["update:modelValue"],
  data() {
    return {
      ServiceType,
      model: [] as PluginConfig[],
      pluginProviders: [],
      pluginLabels: [],
      addFilterModal: false,
      editFilterModal: false,
      editFilterIndex: -1,
      editModel: {
        type: "",
        config: {},
      } as PluginConfig,
      editModelValidation: { errors: [], valid: true },
      filtersEb: null,
    };
  },
  computed: {
    addFilterTitle() {
      return `Add Log Filter Plugin: ${this.subtitle}`;
    },
    editFilterTitle() {
      return `Edit Log Filter Plugin: ${this.subtitle}`;
    },
  },
  watch: {
    modelValue(newVal) {
      this.model = cloneDeep(newVal);
    },
  },
  async mounted() {
    await this.getLogFilterPlugins();
    this.model = cloneDeep(this.modelValue);
    this.filtersEb = mitt();
    if (this.addEvent) {
      eventBus.on(this.addEvent, () => {
        this.addFilter();
      });
    }
    if (this.editEvent) {
      eventBus.on(this.editEvent, (filterIndex: number) => {
        // Validate index is within bounds
        if (typeof filterIndex === 'number' && filterIndex >= 0 && filterIndex < this.model.length) {
          this.editFilterByIndex(filterIndex);
        }
      });
    }
  },
  beforeUnmount() {
    if (this.addEvent) {
      eventBus.off(this.addEvent);
    }
    if (this.editEvent) {
      eventBus.off(this.editEvent);
    }
  },
  methods: {
    editFilterByIndex(index: number) {
      this.editFilterIndex = index;
      this.editModel = cloneDeep(this.model[index]);
      this.filtersEb.emit("edit");
    },
    removeFilterIndex(index: number) {
      this.model.splice(index, 1);
      this.$emit("update:modelValue", this.model);
    },
    clearEdit() {
      this.editFilterIndex = -1;
      this.editModel = { type: "", config: {} };
    },
    addFilter() {
      this.clearEdit();
      this.filtersEb.emit("add");
    },
    async saveEditFilter() {
      try {
        if (this.editFilterIndex === -1) {
          this.model.push(cloneDeep(this.editModel));
        } else {
          //update existing
          this.model[this.editFilterIndex] = cloneDeep(this.editModel);
        }
        this.clearEdit();
        this.$emit("update:modelValue", this.model);
      } catch (e) {
        console.log(e);
      }
    },
    findProvider(type: string) {
      return this.pluginProviders.find((prov) => prov.name === type);
    },
    async getLogFilterPlugins() {
      const response = await getPluginProvidersForService(
        ServiceType.LogFilter,
      );
      if (response.service) {
        this.pluginProviders = response.descriptions;
        this.pluginLabels = response.labels;
      }
    },
  },
});
</script>

<style scoped lang="scss">
.log-filters-container {
  align-items: center;

  display: flex;
  gap: 10px;
  margin-bottom: 10px;

  &.inline {
    margin-bottom: 0;
  }

  .add-gap {
    display: flex;
    gap: 10px;
  }
}
</style>
