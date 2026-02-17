<template>
  <BaseStepCard
    ref="baseStepCard"
    :config="stepConfig"
    :plugin-details="provider || {}"
    :editing="true"
    :show-toggle="depth > 0"
    :initially-expanded="contentExpanded"
    :validation-errors="innerStepValidationErrors"
    card-class="edit-step-card"
    @delete="handleCancel"
    @toggle="contentExpanded = !contentExpanded"
  >
    <template #content>
      <div v-if="isJobRef" class="jobref-form-content">
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

      <div v-else-if="isConditionalLogic" class="conditional-logic-content">
        <div class="step-name-section">
          <label class="text-heading--sm form-label">{{
            $t("editConditionalStep.stepName")
          }}</label>
          <p class="text-body--sm helper-text">
            {{ $t("editConditionalStep.stepNameHelper") }}
          </p>
          <PtInput
            ref="stepDescriptionInput"
            v-model="stepDescription"
            :placeholder="$t('editConditionalStep.stepNamePlaceholder')"
            class="step-name-input"
            data-testid="step-description"
          />
        </div>

        <div class="condition-section">
          <ConditionsEditor
            ref="conditionsEditor"
            v-model="conditionSets"
            :service-name="serviceName"
            :extra-autocomplete-vars="extraAutocompleteVars"
            :depth="depth"
            :validation="validation"
          />
        </div>

        <div class="conditional-divider"></div>

        <div class="steps-section">
          <InnerStepList
            ref="innerStepList"
            v-model="innerCommands"
            :target-service="serviceName"
            :depth="depth + 1"
            :extra-autocomplete-vars="extraAutocompleteVars"
            @update:editing="isEditingInnerStep = $event"
            @update:inner-validation="innerStepValidation = $event"
          />
        </div>
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

      <div v-else-if="loading" class="loading-container">
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
          :disabled="isSaveDisabled"
          @click="handleSave"
        />
      </div>
    </template>
  </BaseStepCard>
</template>

<script lang="ts">
import { defineComponent, defineAsyncComponent, type PropType } from "vue";
import BaseStepCard from "@/library/components/primeVue/StepCards/BaseStepCard.vue";
import pluginConfig from "@/library/components/plugins/pluginConfig.vue";
import PtButton from "@/library/components/primeVue/PtButton/PtButton.vue";
import PtInput from "@/library/components/primeVue/PtInput/PtInput.vue";
import { PluginConfig } from "@/library/interfaces/PluginConfig";
import { getServiceProviderDescription } from "@/library/modules/pluginService";
import { ContextVariable } from "@/library/stores/contextVariables";
import { cloneDeep, merge } from "lodash";
import { getRundeckContext } from "@/library";
import ConditionsEditor from "./ConditionsEditor.vue";
import type { ConditionSet } from "./types/conditionalStepTypes";
import { createEmptyConditionSet } from "./types/conditionalStepTypes";
import type { EditStepData } from "./types/workflowTypes";
import JobRefFormFields from "./JobRefFormFields.vue";
import VueScrollTo from "vue-scrollto";
import { resetValidation } from "./stepEditorUtils";

const rundeckContext = getRundeckContext();

export default defineComponent({
  name: "EditStepCard",
  components: {
    BaseStepCard,
    pluginConfig,
    PtButton,
    PtInput,
    ConditionsEditor,
    InnerStepList: defineAsyncComponent(() => import("./InnerStepList.vue")),
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
    nestedStepToEdit: {
      type: Object as PropType<{
        stepId: string;
        stepIndex: number;
        parentConditionalId?: string;
        parentConditionalIndex?: number;
      } | null>,
      required: false,
      default: null,
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
      editModel: {} as any,
      stepDescription: "",
      provider: null as any,
      loading: false,
      pluginConfigMode: "edit",
      // Conditional logic specific data
      conditionSets: [createEmptyConditionSet()] as ConditionSet[],
      innerCommands: [] as EditStepData[],
      isEditingInnerStep: false,
      innerStepValidation: resetValidation(),
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
    isConditionalLogic() {
      return this.editModel?.type === "conditional.logic";
    },
    stepConfig() {
      // Computed config that includes description for StepCardHeader
      return {
        ...this.editModel,
        description: this.stepDescription,
      };
    },
    isSaveDisabled(): boolean {
      if (!this.isConditionalLogic) {
        return false;
      }
      return this.innerCommands.length === 0 || this.isEditingInnerStep;
    },
    hasJobRefValidationError(): boolean {
      return Boolean(this.validation?.errors?.jobref);
    },
    innerStepValidationErrors() {
      // Only return validation errors if there are actual errors
      return this.innerStepValidation.valid ? undefined : this.innerStepValidation;
    },
  },
  watch: {
    nestedStepToEdit: {
      handler(val) {
        if (val && this.isConditionalLogic) {
          this.$nextTick(() => {
            const innerStepList = this.$refs.innerStepList as any;
            if (innerStepList && typeof innerStepList.editStep === "function") {
              // Check if we need to open a nested conditional first (depth=2 case)
              if (
                val.parentConditionalId &&
                val.parentConditionalIndex !== undefined
              ) {
                // Set nested step info on the InnerStepList so EditStepCard for the
                // intermediate conditional receives it and can open the target step
                innerStepList.nestedStepToEdit = {
                  stepId: val.stepId,
                  stepIndex: val.stepIndex,
                };

                // Open the nested conditional
                innerStepList.editStep(val.parentConditionalIndex);
              } else {
                // Depth=1 case: set the target step info so InnerStepList knows which EditStepCard should scroll
                innerStepList.nestedStepToEdit = {
                  stepId: val.stepId,
                  stepIndex: val.stepIndex,
                };
                // Open the target step
                innerStepList.editStep(val.stepIndex);
              }
            }
          });
        }
      },
      immediate: true,
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

    // Initialize conditional logic data
    if (this.editModel.type === "conditional.logic") {
      this.conditionSets = this.editModel.config?.conditionSets || [
        createEmptyConditionSet(),
      ];
      this.innerCommands = this.editModel.config?.commands || [];

      // Handle nested step editing after editModel is populated
      if (this.nestedStepToEdit) {
        this.$nextTick(() => {
          const innerStepList = this.$refs.innerStepList as any;
          if (innerStepList && typeof innerStepList.editStep === "function") {
            // For depth=2: we only get here if parentConditionalId was already handled by parent
            // So this is the depth=1 case where we need to open the final target
            innerStepList.nestedStepToEdit = {
              stepId: this.nestedStepToEdit.stepId,
              stepIndex: this.nestedStepToEdit.stepIndex,
            };
            innerStepList.editStep(this.nestedStepToEdit.stepIndex);
          }
        });
      }
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
        const baseStepCard = this.$refs.baseStepCard as any;
        const element = baseStepCard?.$el || baseStepCard;

        if (element) {
          VueScrollTo.scrollTo(element, 500, {
            offset: -100,
            easing: "ease-in-out",
            onDone: () => {
              // Focus step description input after scroll completes
              const stepDescriptionInput = this.$refs
                .stepDescriptionInput as any;
              const inputElement =
                stepDescriptionInput?.$el?.querySelector("input") ||
                stepDescriptionInput?.$el;
              inputElement?.focus();
            },
          });
        }
      });
    }
  },
  methods: {
    handleSave() {
      // For conditional logic, build save data with conditions and commands
      if (this.isConditionalLogic) {
        const saveData = {
          ...this.editModel,
          description: this.stepDescription,
          config: {
            ...this.editModel.config,
            conditionSets: this.conditionSets,
            commands: this.innerCommands,
          },
        };
        this.innerStepValidation = resetValidation();
        this.$emit("update:modelValue", saveData);
        this.$emit("save");
        return;
      }

      // Merge description back into editModel before emitting
      const saveData = {
        ...this.editModel,
        description: this.stepDescription,
      };
      this.$emit("update:modelValue", saveData);
      this.$emit("save");
    },
    handleCancel() {
      this.innerStepValidation = resetValidation();
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
          // Don't clear provider on error - keep previous value
        } finally {
          this.loading = false;
        }
      } else {
        // Don't set provider to null if we already have one - prevents header corruption during validation
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

// Conditional logic specific styles
.conditional-logic-content {
  display: flex;
  flex-direction: column;
  gap: var(--sizes-6);

  .condition-section {
    display: flex;
    flex-direction: column;
    gap: var(--sizes-4);
  }

  .conditional-divider {
    height: 1px;
    background: var(--colors-gray-200);
    margin: var(--sizes-2) 0;
  }

  .steps-section {
    display: flex;
    flex-direction: column;
    gap: var(--sizes-4);
  }
}
</style>
