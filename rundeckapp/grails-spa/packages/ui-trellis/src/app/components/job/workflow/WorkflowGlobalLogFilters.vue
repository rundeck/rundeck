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
      :services="['LogFilter']"
      v-model="addFilterModal"
      @cancel="addFilterModal = false"
      @selected="chooseProviderAdd"
      v-if="addFilterModal"
    >
    </choose-plugin-modal>
    <modal
      v-model="editFilterModal"
      title="Edit Log Filter Plugin for Step: All workflow steps"
    >
      <div v-if="findProvider(editFilterType)">
        <plugin-info
          :detail="findProvider(editFilterType)"
          :show-description="true"
          :show-extended="false"
        ></plugin-info>
        <plugin-config
          v-model="editModel"
          :mode="'edit'"
          :service-name="'LogFilter'"
          :provider="editFilterType"
          :show-title="false"
          :show-description="false"
          :context-autocomplete="true"
          :validation="editModelValidation"
          scope="Instance"
          default-scope="Instance"
          group-css=""
        ></plugin-config>
      </div>
      <template #footer>
        <btn @click="cancelEditFilter">Cancel</btn>
        <btn type="success" @click="saveEditFilter">Save</btn>
      </template>
    </modal>
  </div>
</template>
<script lang="ts">
import { GlobalLogFiltersData } from "@/app/components/job/workflow/types/workflowTypes";
import service from "@/app/pages/repository/components/Service.vue";
import { getPluginProvidersForService } from "@/library/modules/pluginService";
import { cloneDeep } from "lodash";
import { defineComponent } from "vue";
import LogFilterButton from "./LogFilterButton.vue";
import pluginConfig from "@/library/components/plugins/pluginConfig.vue";
import pluginInfo from "@/library/components/plugins/PluginInfo.vue";
import ChoosePluginModal from "@/library/components/plugins/ChoosePluginModal.vue";

export default defineComponent({
  name: "WorkflowGlobalLogFilters",
  components: {
    LogFilterButton,
    pluginConfig,
    pluginInfo,
    ChoosePluginModal,
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
      const response = await getPluginProvidersForService("LogFilter");
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
