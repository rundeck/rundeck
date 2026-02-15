<template>
  <BaseStepCard
    ref="baseStepCard"
    :config="stepConfig"
    :plugin-details="provider || {}"
    :expanded="contentExpanded"
    card-class="edit-step-card"
  >
    <template #header>
      <StepCardHeader
        v-if="provider"
        :plugin-details="provider"
        :config="stepConfig"
        :editing="true"
        :show-toggle="depth > 0"
        :expanded="contentExpanded"
        @delete="handleCancel"
        @toggle="contentExpanded = !contentExpanded"
      />
      <div v-else-if="loading" class="loading-container">
        <i class="fas fa-spinner fa-spin"></i>
        <span>{{ $t("loading.text") }}</span>
      </div>
    </template>
    <template #content>
      <div v-if="isJobRef" class="jobref-form-content">
        <div v-if="validationError" class="alert alert-danger">
          {{ validationError }}
        </div>

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
          :show-validation="showRequired"
          :extra-autocomplete-vars="extraAutocompleteVars"
        />
      </div>

      <div v-else-if="isConditionalLogic" class="conditional-logic-content">
        <div class="step-name-section">
          <label class="text-heading--sm form-label">{{ $t("editConditionalStep.stepName") }}</label>
          <p class="text-body--sm helper-text">{{ $t("editConditionalStep.stepNameHelper") }}</p>
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
import StepCardHeader from "@/library/components/primeVue/StepCards/StepCardHeader.vue";
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

const rundeckContext = getRundeckContext();

export default defineComponent({
  name: "EditStepCard",
  components: {
    BaseStepCard,
    pluginConfig,
    PtButton,
    PtInput,
    StepCardHeader,
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
      validationError: "",
      showRequired: false,
      // Conditional logic specific data
      conditionSets: [createEmptyConditionSet()] as ConditionSet[],
      innerCommands: [] as EditStepData[],
      isEditingInnerStep: false,
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
  },
  watch: {
    modelValue: {
      handler(val) {
        if (val && Object.keys(val).length > 0) {
          // Extract description separately
          this.stepDescription = val.description || "";

          // For jobref: merge with defaults (like JobRefForm does)
          // For regular steps: clone as-is
          if (val.jobref) {
            this.editModel = merge(cloneDeep(this.jobRefDefaults), val);
          } else {
            const { description, ...rest } = val;
            this.editModel = cloneDeep(rest);
          }

          // Initialize conditional logic data
          if (this.editModel.type === "conditional.logic") {
            this.conditionSets = this.editModel.config?.conditionSets || [
              createEmptyConditionSet(),
            ];
            this.innerCommands = this.editModel.config?.commands || [];
          }

          this.loadProvider();
        }
      },
      immediate: true,
      deep: true,
    },
    nestedStepToEdit: {
      handler(val) {
        if (val && this.isConditionalLogic) {
          this.$nextTick(() => {
            const innerStepList = this.$refs.innerStepList as any;
            if (innerStepList && typeof innerStepList.editStep === 'function') {
              // Check if we need to open a nested conditional first (depth=2 case)
              if (val.parentConditionalId && val.parentConditionalIndex !== undefined) {
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
            easing: 'ease-in-out',
            onDone: () => {
              // Focus step description input after scroll completes
              const stepDescriptionInput = this.$refs.stepDescriptionInput as any;
              const inputElement = stepDescriptionInput?.$el?.querySelector('input') || stepDescriptionInput?.$el;
              inputElement?.focus();
            }
          });
        }
      });
    }
  },
  methods: {
    handleSave() {
      // Validate job reference before saving
      if (this.isJobRef) {
        if (!this.editModel.jobref.name && !this.editModel.jobref.uuid) {
          this.validationError = this.$t("commandExec.jobName.blank.message");
          this.showRequired = true;
          return;
        }
        this.validationError = "";
        this.showRequired = false;
      }

      // Validate conditional logic before saving
      if (this.isConditionalLogic) {
        const conditionsEditor = this.$refs.conditionsEditor as InstanceType<
          typeof ConditionsEditor
        >;
        if (conditionsEditor && !conditionsEditor.validate()) {
          return;
        }

        const saveData = {
          ...this.editModel,
          description: this.stepDescription,
          config: {
            ...this.editModel.config,
            conditionSets: this.conditionSets,
            commands: this.innerCommands,
          },
        };
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
