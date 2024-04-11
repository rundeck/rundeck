<template>
  <div>
    <template v-if="showIfEmpty || model.length > 0">{{ title }}</template>
    <template v-for="(entry, i) in model">
      <LogFilterButton
        :plugin-description="findProvider(entry.type)"
        v-if="findProvider(entry.type)"
        @editFilter="editFilterByIndex(i)"
        @removeFilter="removeFilterIndex(i)"
      ></LogFilterButton>
    </template>
    <log-filter-controls
      v-model="editModel"
      :title="title"
      :subtitle="subtitle"
      :event-bus="filtersEb"
      @update:modelValue="saveEditFilter"
      @cancel="clearEdit"
      :show-button="showIfEmpty || model.length > 0"
      v-if="filtersEb"
    />
  </div>
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
  },
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
    saveEditFilter() {
      //todo: validate
      if (this.editFilterIndex === -1) {
        this.model.push(cloneDeep(this.editModel));
      } else {
        //update existing
        this.model[this.editFilterIndex] = cloneDeep(this.editModel);
      }
      this.clearEdit();
      this.$emit("update:modelValue", this.model);
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
  async mounted() {
    await this.getLogFilterPlugins();
    this.model = cloneDeep(this.modelValue);
    this.filtersEb = mitt();
    if (this.addEvent) {
      eventBus.on(this.addEvent, () => {
        this.addFilter();
      });
    }
  },
  beforeUnmount() {
    if (this.addEvent) {
      eventBus.off(this.addEvent);
    }
  },
});
</script>

<style scoped lang="scss"></style>
