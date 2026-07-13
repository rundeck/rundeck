<template>
  <div
    class="inline-plugin-config-form"
    data-testid="inline-plugin-config-form"
  >
    <div v-if="provider">
      <p>
        <plugin-info
          :detail="provider"
          :show-description="true"
          :show-extended="false"
          description-css="ml-5"
        ></plugin-info>
      </p>
      <plugin-config
        :model-value="editModel"
        :mode="pluginConfigMode"
        :plugin-config="provider"
        :show-title="false"
        :show-description="false"
        :context-autocomplete="true"
        :validation="validation"
        scope="Instance"
        default-scope="Instance"
        group-css=""
        description-css="ml-5"
        data-testid="plugin-info"
        :service-name="serviceName"
        :extra-autocomplete-vars="extraAutocompleteVars"
        @update:model-value="onPluginConfigUpdate"
      ></plugin-config>
      <slot name="extra"></slot>
    </div>
    <div v-else-if="loading">
      <p>
        <i class="fas fa-spinner fa-spin"></i>
        {{ $t("loading.text") }}
      </p>
    </div>
    <div v-if="showButtons" class="inline-form-footer">
      <btn data-testid="cancel-button" @click="$emit('cancel')">{{
        $t("Cancel")
      }}</btn>
      <btn data-testid="save-button" type="success" @click="saveChanges">{{
        $t("Save")
      }}</btn>
    </div>
  </div>
</template>
<script lang="ts">
import pluginConfig from "./pluginConfig.vue";
import pluginInfo from "./PluginInfo.vue";
import { PluginConfig } from "../../../library/interfaces/PluginConfig";
import { getServiceProviderDescription } from "../../../library/modules/pluginService";
import { ContextVariable } from "../../../library/stores/contextVariables";
import { cloneDeep } from "lodash";
import { defineComponent, type PropType } from "vue";

export default defineComponent({
  name: "InlinePluginConfigForm",
  components: { pluginInfo, pluginConfig },
  props: {
    modelValue: {
      type: Object as PropType<PluginConfig>,
      required: true,
      default: () => ({}) as PluginConfig,
    },
    serviceName: {
      type: String,
      required: true,
    },
    validation: {
      type: Object,
      required: false,
      default: () => ({}),
    },
    extraAutocompleteVars: {
      type: Array as PropType<ContextVariable[]>,
      required: false,
      default: () => [],
    },
    showButtons: {
      type: Boolean,
      default: false,
    },
  },
  emits: ["cancel", "save", "update:modelValue"],
  data() {
    return {
      editModel: {} as PluginConfig,
      provider: null,
      loading: false,
      pluginConfigMode: "edit",
    };
  },
  watch: {
    async modelValue(val) {
      this.editModel = cloneDeep(val);
      await this.loadProvider();
    },
  },
  async mounted() {
    this.editModel = cloneDeep(this.modelValue);
    if (
      this.modelValue.config &&
      Object.keys(this.modelValue.config).length === 0
    ) {
      this.pluginConfigMode = "create";
    }
    await this.loadProvider();
  },
  methods: {
    /**
     * Returns the current form data for parent components that handle save
     * (e.g. EditStepCard with showButtons=false).
     */
    getFormData() {
      return this.editModel;
    },
    onPluginConfigUpdate(updated: PluginConfig) {
      // pluginConfig only emits plugin fields (type/config). Merge instead of
      // replacing so caller-owned fields (e.g. keepgoingOnSuccess, nodeStep, id
      // on an error handler) are preserved rather than dropped.
      this.editModel = { ...this.editModel, ...updated };
      this.$emit("update:modelValue", this.editModel);
    },
    saveChanges() {
      this.$emit("update:modelValue", this.editModel);
      this.$emit("save");
    },
    async loadProvider() {
      if (this.editModel.type) {
        try {
          this.loading = true;
          this.provider = await getServiceProviderDescription(
            this.serviceName,
            this.editModel.type,
          );
        } catch (e) {
          console.error("[InlinePluginConfigForm] loadProvider error:", e);
        } finally {
          this.loading = false;
        }
      } else {
        this.loading = false;
        this.provider = null;
      }
    },
  },
});
</script>
