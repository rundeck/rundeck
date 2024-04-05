<template>
  <div>
    Global Log Filters
    <btn size="sm" @click="addFilterModal = true">
      <i class="glyphicon glyphicon-plus"></i>
      Add
    </btn>
  </div>
  <div>
    <template v-for="(entry, i) in modelValue.filters">
      <LogFilterButton
        :plugin-description="findProvider(entry.type)"
        v-if="findProvider(entry.type)"
        @editFilter="editFilterByIndex(i)"
        @removeFilter="removeFilterIndex(i)"
      ></LogFilterButton>
    </template>
    <choose-plugin-modal
      title="Add Log Filter Plugin for Step: All workflow steps"
      :services="[ServiceType.LogFilter]"
      v-model="addFilterModal"
      @cancel="addFilterModal = false"
      @selected="chooseProviderAdd"
      v-if="addFilterModal"
    >
    </choose-plugin-modal>
    <edit-plugin-modal
      v-if="editFilterModal"
      v-model:modal-active="editFilterModal"
      v-model="editModel"
      :validation="editModelValidation"
      :service-name="ServiceType.LogFilter"
      title="Edit Log Filter Plugin for Step: All workflow steps"
      @cancel="cancelEditFilter"
      @save="saveEditFilter"
    ></edit-plugin-modal>
  </div>
</template>
<script lang="ts">
import { GlobalLogFiltersData } from "@/app/components/job/workflow/types/workflowTypes";
import { getPluginProvidersForService } from "@/library/modules/pluginService";
import { cloneDeep } from "lodash";
import { defineComponent } from "vue";
import LogFilterButton from "./LogFilterButton.vue";
import pluginConfig from "@/library/components/plugins/pluginConfig.vue";
import pluginInfo from "@/library/components/plugins/PluginInfo.vue";
import ChoosePluginModal from "@/library/components/plugins/ChoosePluginModal.vue";
import EditPluginModal from "@/library/components/plugins/EditPluginModal.vue";
import { ServiceType } from "@/library/stores/Plugins";

export default defineComponent({
  name: "WorkflowGlobalLogFilters",
  components: {
    LogFilterButton,
    pluginConfig,
    pluginInfo,
    ChoosePluginModal,
    EditPluginModal,
  },
  props: {
    modelValue: {
      type: Object,
      required: true,
      default: () => ({}) as GlobalLogFiltersData,
    },
  },
  data() {
    return {
      ServiceType,
      model: { filters: [] } as GlobalLogFiltersData,
      pluginProviders: [],
      pluginLabels: [],
      addFilterModal: false,
      editFilterModal: false,
      editFilterIndex: -1,
      editFilterType: "",
      editModel: {
        type: "",
        config: {},
      },
      editModelValidation: { errors: [], valid: true },
    };
  },
  methods: {
    clearEdit() {
      this.editFilterType = "";
      this.editModel = { type: "", config: {} };
      this.editFilterIndex = -1;
      this.editModelValidation = { errors: [], valid: true };
    },
    chooseProviderAdd({
      service,
      provider,
    }: {
      service: string;
      provider: string;
    }) {
      this.addFilterModal = false;
      this.editFilterType = provider;
      this.editFilterIndex = -1;
      this.editModel = { type: provider, config: {} };
      this.editFilterModal = true;
    },
    editFilterByIndex(index: number) {
      this.editFilterIndex = index;
      this.editFilterType = this.model.filters[index].type;
      this.editModel = cloneDeep(this.model.filters[index]);
      this.editFilterModal = true;
    },
    removeFilterIndex(index: number) {
      this.model.filters.splice(index, 1);
      this.$emit("update:modelValue", this.model);
    },
    cancelEditFilter() {
      this.editFilterModal = false;
      this.editFilterType = "";
      this.editModel = { type: "", config: {} };
    },
    saveEditFilter() {
      //todo: validate
      this.editFilterModal = false;
      if (this.editFilterIndex === -1) {
        this.model.filters.push(cloneDeep(this.editModel));
      } else {
        //update existing
        this.model.filters[this.editFilterIndex] = cloneDeep(this.editModel);
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
  },
});
</script>
