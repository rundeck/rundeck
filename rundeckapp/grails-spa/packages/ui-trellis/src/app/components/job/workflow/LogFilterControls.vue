<template>
  <btn size="sm" @click="addFilter" v-if="showButton">
    <i class="glyphicon glyphicon-plus"></i>
    {{ $t("message_add") }}
  </btn>
  <choose-plugin-modal
    :title="addFilterTitle"
    :services="[ServiceType.LogFilter]"
    v-model="addFilterModal"
    @cancel="addFilterModal = false"
    @selected="chooseProviderAdd"
    v-if="addFilterModal"
  >
  </choose-plugin-modal>
  <edit-plugin-modal
    v-model:modal-active="editFilterModal"
    v-model="model"
    :validation="editModelValidation"
    :service-name="ServiceType.LogFilter"
    :title="editFilterTitle"
    @cancel="cancelEditFilter"
    @save="saveEditFilter"
  ></edit-plugin-modal>
</template>
<script lang="ts">
import { getRundeckContext } from "@/library";
import ChoosePluginModal from "@/library/components/plugins/ChoosePluginModal.vue";
import EditPluginModal from "@/library/components/plugins/EditPluginModal.vue";
import { PluginConfig } from "@/library/interfaces/PluginConfig";
import { getPluginProvidersForService } from "@/library/modules/pluginService";
import { ServiceType } from "@/library/stores/Plugins";
import { cloneDeep } from "lodash";
import { defineComponent, nextTick } from "vue";
export default defineComponent({
  name: "LogFilterControls",
  components: { ChoosePluginModal, EditPluginModal },
  emits: ["update:modelValue", "cancel"],
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
      //todo: validate
      this.$emit("update:modelValue", this.model);
      this.editFilterModal = false;
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
});
</script>

<style scoped lang="scss"></style>
