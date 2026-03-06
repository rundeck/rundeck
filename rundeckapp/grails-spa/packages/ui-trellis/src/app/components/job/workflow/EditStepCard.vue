<template>
  <BaseStepCard
    ref="baseStepCard"
    :config="stepConfig"
    :plugin-details="provider || {}"
    :editing="true"
    :show-toggle="depth > 0"
    :initially-expanded="contentExpanded"
    :validation-errors="validationErrors"
    card-class="edit-step-card"
    @delete="handleCancel"
    @toggle="contentExpanded = !contentExpanded"
  >
    <template #content>
      <div v-if="isJobRef" class="jobref-form-content" data-testid="jobref-form-content">
        <div class="step-name-section">
          <div class="form-group">
            <label
              class="col-sm-2 control-label input-sm"
              for="stepDescription"
            >
              {{ $t("Workflow.step.property.description.label") }}
            </label>
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
              {{ $t("Workflow.step.property.description.help") }}
            </div>
          </div>
        </div>

        <JobRefFormFields
          v-model="editModel.jobref"
          :show-validation="hasJobRefValidationError"
          :extra-autocomplete-vars="extraAutocompleteVars"
        />
      </div>

      <div v-else-if="provider">
        <div class="step-name-section">
          <div class="form-group">
            <label
              class="col-sm-2 control-label input-sm"
              for="stepDescription"
              >{{ $t("Workflow.step.property.description.label") }}</label
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
              {{ $t("Workflow.step.property.description.help") }}
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

      <div v-else-if="loading" class="loading-container" data-testid="loading-container">
        <i class="fas fa-spinner fa-spin"></i>
        <span>{{ $t("loading.text") }}</span>
      </div>
    </template>
    <template #footer>
      <div
        v-show="depth === 0 || contentExpanded"
        class="edit-step-card-footer"
      >
        <PtButton
          outlined
          severity="secondary"
          :label="$t('Cancel')"
          data-testid="cancel-button"
          @click="handleCancel"
        />
        <PtButton
          outlined
          :label="$t('Save')"
          data-testid="save-button"
          @click="handleSave"
        />
      </div>
    </template>
  </BaseStepCard>
</template>

<script lang="ts">
import { defineComponent, type PropType } from "vue";
import BaseStepCard from "../../../../library/components/primeVue/StepCards/BaseStepCard.vue";
import pluginConfig from "../../../../library/components/plugins/pluginConfig.vue";
import PtButton from "../../../../library/components/primeVue/PtButton/PtButton.vue";
import { PluginConfig } from "../../../../library/interfaces/PluginConfig";
import { getServiceProviderDescription } from "../../../../library/modules/pluginService";
import { ContextVariable } from "../../../../library/stores/contextVariables";
import { cloneDeep, merge } from "lodash";
import { getRundeckContext } from "../../../../library";
import type { EditStepData } from "./types/workflowTypes";
import JobRefFormFields from "./JobRefFormFields.vue";
import VueScrollTo from "vue-scrollto";
import { resetValidation, type PluginDetails } from "./stepEditorUtils";

const rundeckContext = getRundeckContext();

export default defineComponent({
  name: "EditStepCard",
  components: {
    BaseStepCard,
    pluginConfig,
    PtButton,
    JobRefFormFields,
  },
  provide() {
    return {
      showJobsAsLinks: false,
    };
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
    pluginDetails: {
      type: Object,
      required: false,
      default: null,
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
    depth: {
      type: Number,
      default: 0,
    },
    shouldScrollIntoView: {
      type: Boolean,
      default: false,
    },
  },
  emits: ["cancel", "save", "update:modelValue"],
  data() {
    return {
      contentExpanded: true,
      editModel: {} as EditStepData,
      stepDescription: "",
      provider: null as PluginDetails | null,
      loading: false,
      pluginConfigMode: "edit",
      validationErrors: resetValidation(),
      // Job reference defaults (matching JobRefForm)
      jobRefDefaults: {
        description: "",
        jobref: {
          nodeStep: false,
          name: "",
          uuid: "",
          project: rundeckContext.projectName,
          group: "",
          args: "",
          failOnDisable: false,
          childNodes: false,
          importOptions: false,
          ignoreNotifications: false,
          nodefilters: {
            filter: "",
            dispatch: {
              threadcount: null,
              keepgoing: null,
              rankAttribute: null,
              rankOrder: null,
              nodeIntersect: null,
            },
          },
        },
      },
    };
  },
  computed: {
    isJobRef() {
      return Boolean(this.editModel?.jobref);
    },
    stepConfig() {
      // Computed config that includes description for StepCardHeader
      return {
        ...this.editModel,
        description: this.stepDescription,
      };
    },
    hasJobRefValidationError(): boolean {
      return Boolean(this.validation?.errors?.jobref);
    },
  },
  async mounted() {
    this.stepDescription = this.modelValue.description || "";

    // For jobref: merge with defaults (like JobRefForm does)
    // For regular steps: clone as-is
    if (this.modelValue.jobref) {
      this.editModel = merge(cloneDeep(this.jobRefDefaults), this.modelValue);
    } else {
      const { description, ...rest } = this.modelValue;
      this.editModel = cloneDeep(rest);
    }

    if (
      this.modelValue.config &&
      Object.keys(this.modelValue.config).length === 0
    ) {
      this.pluginConfigMode = "create";
    }

    await this.loadProvider();

    // Scroll into view if this step was opened via click-to-edit
    if (this.shouldScrollIntoView) {
      this.$nextTick(() => {
        setTimeout(() => {
          const baseStepCard = this.$refs.baseStepCard as any;
          const element = baseStepCard?.$el || baseStepCard;

          if (element) {
            VueScrollTo.scrollTo(element, 500, {
              offset: -100,
              easing: "ease-in-out",
              onDone: () => {
                setTimeout(() => {
                  const inputElement = document.getElementById("stepDescription");
                  if (inputElement) {
                    inputElement.focus({ preventScroll: true });
                  }
                }, 100);
              },
            });
          }
        }, 150);
      });
    }
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
      // If plugin-details prop is provided, use it (no need to fetch)
      if (this.pluginDetails) {
        this.provider = this.pluginDetails;
        this.loading = false;
        return;
      }

      // Otherwise, fetch from API (existing behavior for backwards compatibility)
      if (this.editModel.type) {
        try {
          this.loading = true;
          this.provider = await getServiceProviderDescription(
            this.serviceName,
            this.editModel.type,
          );
        } catch (e) {
          console.error("Error loading provider description:", e);
        } finally {
          this.loading = false;
        }
      } else {
        this.loading = false;
        console.warn(
          "loadProvider called without editModel.type - keeping existing provider",
        );
      }
    },
  },
});
</script>

<style lang="scss">
.edit-step-card {
  margin-bottom: var(--sizes-4);

  &-footer {
    display: flex;
    justify-content: flex-end;
    gap: var(--sizes-2);
    padding: var(--sizes-4);
    border-top: 1px solid var(--colors-gray-300-original);
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
