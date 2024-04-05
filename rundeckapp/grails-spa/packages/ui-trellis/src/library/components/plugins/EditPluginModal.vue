<template>
  <modal v-model="showModal" :title="title || $t('plugin.edit.title')">
    <div v-if="provider">
      <plugin-info
        :detail="provider"
        :show-description="true"
        :show-extended="false"
      ></plugin-info>
      <plugin-config
        v-model="editModel"
        :mode="'edit'"
        :service-name="serviceName"
        :provider="editModel.type"
        :show-title="false"
        :show-description="false"
        :context-autocomplete="true"
        :validation="validation"
        scope="Instance"
        default-scope="Instance"
        group-css=""
      ></plugin-config>
    </div>
    <div v-else>
      <p>
        <i class="fas fa-spinner fa-spin"></i>
        {{ $t("loading.text") }}
      </p>
    </div>
    <template #footer>
      <btn @click="$emit('cancel')">{{ $t("Cancel") }}</btn>
      <btn type="success" @click="saveChanges">{{ $t("Save") }}</btn>
    </template>
  </modal>
</template>
<script lang="ts">
import pluginConfig from "@/library/components/plugins/pluginConfig.vue";
import pluginInfo from "@/library/components/plugins/PluginInfo.vue";
import { PluginConfig } from "@/library/interfaces/PluginConfig";
import { getServiceProviderDescription } from "@/library/modules/pluginService";
import { cloneDeep } from "lodash";
import { defineComponent } from "vue";

export default defineComponent({
  name: "EditPluginModal",
  components: { pluginInfo, pluginConfig },
  emits: ["cancel", "save", "update:modelValue", "update:modalActive"],
  props: {
    title: {
      type: String,
      required: true,
    },
    modelValue: {
      type: Object,
      required: true,
      default: () => ({}) as PluginConfig,
    },
    serviceName: {
      type: String,
      required: true,
    },
    modalActive: {
      type: Boolean,
      required: false,
      default: false,
    },
    validation: {
      type: Object,
      required: false,
      default: () => ({}),
    },
  },
  data() {
    return {
      showModal: this.modalActive,
      editModel: {} as PluginConfig,
      provider: null,
    };
  },
  methods: {
    async saveChanges() {
      this.$emit("update:modelValue", this.editModel);
      this.$emit("save");
    },
  },
  async mounted() {
    this.editModel = cloneDeep(this.modelValue);
    this.provider = await getServiceProviderDescription(
      this.serviceName,
      this.editModel.type,
    );
  },
});
</script>
<style scoped lang="scss"></style>
