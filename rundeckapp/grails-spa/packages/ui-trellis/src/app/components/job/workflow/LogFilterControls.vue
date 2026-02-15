<template>
  <btn v-if="showButton" size="sm" @click="addFilter">
    <i class="glyphicon glyphicon-plus"></i>
    {{ $t("message_add") }}
  </btn>
  <Teleport to="body">
    <choose-plugin-modal
      v-if="addFilterModal"
      v-model="addFilterModal"
      :title="addFilterTitle"
      :services="[ServiceType.LogFilter]"
      @cancel="addFilterModal = false"
      @selected="chooseProviderAdd"
    >
    </choose-plugin-modal>
  </Teleport>
  <Teleport to="body">
    <edit-plugin-modal
      v-model:modal-active="editFilterModal"
      v-model="model"
      :validation="editModelValidation"
      :service-name="ServiceType.LogFilter"
      :title="editFilterTitle"
      @cancel="cancelEditFilter"
      @save="saveEditFilter"
    ></edit-plugin-modal>
  </Teleport>
</template>
<script lang="ts">
import ChoosePluginModal from "@/library/components/plugins/ChoosePluginModal.vue";
import EditPluginModal from "@/library/components/plugins/EditPluginModal.vue";
import { PluginConfig } from "@/library/interfaces/PluginConfig";
import {
  getPluginProvidersForService,
  validatePluginConfig,
} from "@/library/modules/pluginService";
import { ServiceType } from "@/library/stores/Plugins";
import { cloneDeep } from "lodash";
import { defineComponent, nextTick } from "vue";
export default defineComponent({
  name: "LogFilterControls",
  components: { ChoosePluginModal, EditPluginModal },
  props: {
    modelValue: {
      type: Object,
      required: true,
      default: () => ({}) as PluginConfig,
    },
    showButton: {
      type: Boolean,
      required: false,
      default: true,
    },
    title: {
      type: String,
      required: true,
    },
    subtitle: {
      type: String,
      required: true,
    },
    eventBus: {
      type: Object,
      required: true,
    },
  },
  emits: ["update:modelValue", "cancel"],
  data() {
    return {
      ServiceType,
      pluginProviders: [],
      pluginLabels: [],
      addFilterModal: false,
      editFilterModal: false,
      model: {
        type: "",
        config: {},
      } as PluginConfig,
      editModelValidation: { errors: [], valid: true },
    };
  },
  computed: {
    addFilterTitle() {
      return `Add Log Filter Plugin for: ${this.subtitle}`;
    },
    editFilterTitle() {
      return `Edit Log Filter Plugin for: ${this.subtitle}`;
    },
  },
  watch: {
    modelValue(val) {
      this.model = cloneDeep(val);
    },
    editFilterModal(val) {
      if (!val) {
        this.clearEdit();
        this.$emit("cancel");
      }
    },
  },
  async mounted() {
    await this.getLogFilterPlugins();
    this.model = cloneDeep(this.modelValue);
    this.eventBus.on("edit", () => {
      if (!this.addFilterModal) {
        this.editFilterModal = true;
      }
    });
    this.eventBus.on("add", this.addFilter);
  },
  methods: {
    clearEdit() {
      this.model = { type: "", config: {} };
      this.editModelValidation = { errors: [], valid: true };
    },
    addFilter() {
      if (this.editFilterModal) {
        return;
      }
      this.clearEdit();
      this.addFilterModal = true;
    },
    chooseProviderAdd({
      service,
      provider,
    }: {
      service: string;
      provider: string;
    }) {
      this.addFilterModal = false;
      this.model = { type: provider, config: {} };
      this.editFilterModal = true;
    },
    async cancelEditFilter() {
      this.editFilterModal = false;
    },
    async saveEditFilter() {
      const response = await validatePluginConfig(
        ServiceType.LogFilter,
        this.model.type,
        this.model.config,
      );

      if (response.valid && Object.keys(response.errors || {}).length === 0) {
        this.$emit("update:modelValue", this.model);
        this.editFilterModal = false;
      } else {
        this.editModelValidation.valid = response.valid;
        this.editModelValidation.errors = response.errors;
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

<style scoped lang="scss"></style>
