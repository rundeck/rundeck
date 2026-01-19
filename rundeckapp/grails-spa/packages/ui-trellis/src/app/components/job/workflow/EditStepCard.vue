<template>
  <Card class="edit-step-card">
    <template #header>
      <div class="edit-step-card-header">
        <div class="edit-step-card-header-title">
          <plugin-info
            v-if="provider"
            :detail="provider"
            :show-description="false"
            :show-extended="false"
          />
          <div v-else-if="loading" class="loading-container">
            <i class="fas fa-spinner fa-spin"></i>
            <span>{{ $t("loading.text") }}</span>
          </div>
        </div>
        <PtButton
          outlined
          severity="secondary"
          icon="pi pi-times"
          :aria-label="$t('Cancel')"
          @click="handleCancel"
        />
      </div>
    </template>
    <template #content>
      <div v-if="provider">
        <div class="step-name-section">
          <PtInput
            input-id="stepDescription"
            data-testid="step-description"
            v-model="editModel.description"
            type="text"
            label="Step Name"
            help-text="Name for this step"
          />
        </div>
        <plugin-config
          v-model="editModel"
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
        />
      </div>
      <div v-else-if="loading" class="loading-container">
        <i class="fas fa-spinner fa-spin"></i>
        <span>{{ $t("loading.text") }}</span>
      </div>
    </template>
    <template #footer>
      <div class="edit-step-card-footer">
        <PtButton
          outlined
          severity="secondary"
          :label="$t('Cancel')"
          data-testid="cancel-button"
          @click="handleCancel"
        />
        <PtButton
          severity="primary"
          :label="$t('Save')"
          data-testid="save-button"
          @click="handleSave"
        />
      </div>
    </template>
  </Card>
</template>

<script lang="ts">
import { defineComponent, type PropType } from "vue";
import Card from "primevue/card";
import pluginConfig from "@/library/components/plugins/pluginConfig.vue";
import pluginInfo from "@/library/components/plugins/PluginInfo.vue";
import PtButton from "@/library/components/primeVue/PtButton/PtButton.vue";
import PtInput from "@/library/components/primeVue/PtInput/PtInput.vue";
import { PluginConfig } from "@/library/interfaces/PluginConfig";
import { getServiceProviderDescription } from "@/library/modules/pluginService";
import { ContextVariable } from "@/library/stores/contextVariables";
import { cloneDeep } from "lodash";

export default defineComponent({
  name: "EditStepCard",
  components: {
    Card,
    pluginInfo,
    pluginConfig,
    PtButton,
    PtInput,
  },
  props: {
    modelValue: {
      type: Object,
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
  },
  emits: ["cancel", "save", "update:modelValue"],
  data() {
    return {
      editModel: {} as PluginConfig,
      provider: null as any,
      loading: false,
      pluginConfigMode: "edit",
    };
  },
  watch: {
    modelValue: {
      handler(val) {
        if (val && Object.keys(val).length > 0) {
          this.editModel = cloneDeep(val);
          this.loadProvider();
        }
      },
      immediate: true,
      deep: true,
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
    handleSave() {
      // Emit the updated model first, then save event
      // This ensures parent's editModel is updated before save handler runs
      this.$emit("update:modelValue", this.editModel);
      this.$emit("save");
    },
    handleCancel() {
      this.$emit("cancel");
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
          console.log(e);
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

<style lang="scss" scoped>
.edit-step-card {
  box-shadow: none;
  overflow: hidden;
  border-radius: var(--radii-md);
  border: 1px solid var(--colors-gray-300);
  margin-bottom: var(--sizes-4);

  .p-card-body {
    padding: var(--sizes-4);
  }

  &-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: var(--sizes-4);
    background-color: var(--colors-secondaryBackgroundOnLight);
    border-bottom: 2px solid var(--colors-gray-300);

    &-title {
      flex: 1;
    }
  }

  &-footer {
    display: flex;
    justify-content: flex-end;
    gap: var(--sizes-2);
    padding: var(--sizes-4);
    border-top: 1px solid var(--colors-gray-300);
  }
}

.loading-container {
  display: flex;
  align-items: center;
  gap: var(--sizes-2);
  padding: var(--sizes-4);
}

.step-name-section {
  display: flex;
  flex-direction: column;
  gap: var(--sizes-1);
  margin-bottom: var(--sizes-4);
}
</style>
