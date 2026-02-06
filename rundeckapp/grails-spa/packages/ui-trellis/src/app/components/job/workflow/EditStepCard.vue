<template>
  <Card class="edit-step-card">
    <template #header>
        <StepCardHeader
          v-if="provider"
          :plugin-details="provider"
          :config="stepConfig"
          :hide-step-type="true"
          @delete="handleCancel"
        />
      <div v-else-if="loading" class="loading-container">
        <i class="fas fa-spinner fa-spin"></i>
        <span>{{ $t("loading.text") }}</span>
      </div>
    </template>
    <template #content>
      <div v-if="provider">
        <div class="step-name-section">
          <div class="form-group">
            <label
              class="col-sm-2 control-label input-sm"
              for="stepDescription"
              >Step Name</label
            >
            <div class="col-sm-10">
              <input
                id="stepDescription"
                data-testid="step-description"
                v-model="stepDescription"
                type="text"
                name="stepDescription"
                size="100"
                class="form-control input-sm"
              />
            </div>
            <div class="col-sm-10 col-sm-offset-2 help-block">
              Name for this step
            </div>
          </div>
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
import PtButton from "@/library/components/primeVue/PtButton/PtButton.vue";
import StepCardHeader from "@/library/components/primeVue/StepCards/StepCardHeader.vue";
import { PluginConfig } from "@/library/interfaces/PluginConfig";
import { getServiceProviderDescription } from "@/library/modules/pluginService";
import { ContextVariable } from "@/library/stores/contextVariables";
import { cloneDeep } from "lodash";

export default defineComponent({
  name: "EditStepCard",
  components: {
    Card,
    pluginConfig,
    PtButton,
    StepCardHeader,
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
    showNavigation: {
      type: Boolean,
      default: false,
    },
  },
  emits: ["cancel", "save", "update:modelValue"],
  data() {
    return {
      editModel: {} as any,
      stepDescription: "",
      provider: null as any,
      loading: false,
      pluginConfigMode: "edit",
    };
  },
  computed: {
    stepConfig() {
      // Computed config that includes description for StepCardHeader
      return {
        ...this.editModel,
        description: this.stepDescription,
      };
    },
  },
  watch: {
    modelValue: {
      handler(val) {
        if (val && Object.keys(val).length > 0) {
          // Extract description separately, keep everything else in editModel
          this.stepDescription = val.description || "";
          const { description, ...rest } = val;
          this.editModel = cloneDeep(rest);
          this.loadProvider();
        }
      },
      immediate: true,
      deep: true,
    },
  },
  async mounted() {
    this.stepDescription = this.modelValue.description || "";
    const { description, ...rest } = this.modelValue;
    this.editModel = cloneDeep(rest);
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
      // Merge description back into editModel before emitting
      const saveData = {
        ...this.editModel,
        description: this.stepDescription,
      };
      this.$emit("update:modelValue", saveData);
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
