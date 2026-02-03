<template>
  <div class="edit-conditional-step-card">
    <Card>
      <template #header>
        <div class="card-header">
          <div class="title-section">
            <div class="title-with-icon">
              <img
                src="@/library/theme/images/icon-condition.png"
                alt="Condition"
                class="condition-icon"
              />
              <h2 class="text-heading--lg card-title">{{ cardTitle }}</h2>
            </div>
            <p class="text-body text-body--secondary card-description">
              {{ cardDescription }}
            </p>
          </div>
          <PtButton
            text
            severity="secondary"
            icon="pi pi-times"
            class="close-button"
            @click="handleCancel"
          />
        </div>
      </template>
      <template #content>
        <div class="step-name-section">
          <label class="text-heading--sm form-label">{{ $t("editConditionalStep.stepName") }}</label>
          <p class="text-body--sm helper-text">{{ $t("editConditionalStep.stepNameHelper") }}</p>
          <PtInput
            v-model="stepName"
            :placeholder="$t('editConditionalStep.stepNamePlaceholder')"
            class="step-name-input"
          />
        </div>

        <div class="condition-section">
          <div>
            <h3 class="text-heading--md section-title">{{ $t("editConditionalStep.defineCondition") }}</h3>
            <p class="text-body text-body--secondary section-description">
              {{ $t("editConditionalStep.defineConditionHelper") }}
            </p>
          </div>

          <div
            v-for="(conditionSet, setIndex) in conditionSets"
            :key="conditionSet.id"
            class="condition-set-container"
          >
            <div v-if="setIndex > 0" class="or-separator">
              <span class="or-label">{{ $t("editConditionalStep.or") }}</span>
            </div>

            <div class="condition-set">
              <h4 class="condition-set-title">
                {{ $t("editConditionalStep.conditionNumber", { number: setIndex + 1 }) }}
              </h4>

              <div class="conditions-list">
                <div class="conditions-connector" :class="{ 'has-multiple': conditionSet.conditions.length > 1 }">
                  <template v-for="(condition, condIndex) in conditionSet.conditions" :key="condition.id">
                    <div v-if="condIndex > 0" class="and-separator">
                      <span class="and-label">{{ $t("editConditionalStep.and") }}</span>
                    </div>
                    <ConditionRow
                      :condition="condition"
                      :field-options="fieldOptions"
                      :operator-options="operatorOptions"
                      :show-labels="condIndex === 0"
                      :show-delete-button="conditionSet.conditions.length > 1 || conditionSets.length > 1"
                      :service-name="serviceName"
                      :suggestions="autocompleteSuggestions"
                      :tab-mode="autocompleteSuggestions.length > 0"
                      @update:condition="(updated) => updateCondition(setIndex, condIndex, updated)"
                      @delete="() => removeCondition(setIndex, condIndex)"
                      @switch-step-type="handleSwitchStepType"
                    />
                  </template>
                </div>
              </div>

              <div v-if="canAddCondition(setIndex)" class="condition-actions" :class="{ 'has-multiple': conditionSet.conditions.length > 1 }">
                <button type="button" class="btn-add-link" @click="addCondition(setIndex)">
                  <i class="pi pi-plus"></i>
                  <span>{{ $t("editConditionalStep.add") }}</span>
                </button>
              </div>
            </div>
          </div>

          <div v-if="canAddConditionSet" class="condition-set-actions">
            <PtButton
              outlined
              severity="secondary"
              icon="pi pi-plus"
              :label="$t('editConditionalStep.addConditionSet')"
              class="btn-add-condition-set"
              @click="addConditionSet"
            />
          </div>
        </div>

        <div class="divider"></div>

        <div class="steps-section">
          <h3 class="text-heading--md section-title">{{ $t("editConditionalStep.setSteps") }}</h3>
          <PtButton
            outlined
            severity="secondary"
            icon="pi pi-plus"
            :label="$t('editConditionalStep.addConditionStep')"
            class="btn-add-condition-step"
            @click="handleAddConditionStep"
          />
        </div>
      </template>
      <template #footer>
        <div class="card-footer">
          <PtButton
            outlined
            severity="secondary"
            :label="$t('editConditionalStep.cancel')"
            class="btn-cancel"
            @click="handleCancel"
          />
          <PtButton
            outlined
            :label="$t('editConditionalStep.saveStep')"
            class="btn-save"
            @click="handleSave"
          />
        </div>
      </template>
    </Card>
  </div>
</template>

<script lang="ts">
import { defineComponent, type PropType } from "vue";
import Card from "primevue/card";
import PtInput from "@/library/components/primeVue/PtInput/PtInput.vue";
import PtButton from "@/library/components/primeVue/PtButton/PtButton.vue";
import ConditionRow from "./ConditionRow.vue";
import { contextVariables, type ContextVariable } from "@/library/stores/contextVariables";
import { ServiceType } from "@/library/stores/Plugins";
import { cloneDeep } from "lodash";
import {
  type Condition,
  type ConditionSet,
  type OperatorOption,
  type FieldOption,
  MAX_CONDITIONS_PER_SET,
  MAX_CONDITION_SETS,
  createEmptyCondition,
  createEmptyConditionSet,
} from "./types/conditionalStepTypes";
import type { EditStepData } from "./types/workflowTypes";

export default defineComponent({
  name: "EditConditionalStepCard",
  components: {
    Card,
    PtInput,
    PtButton,
    ConditionRow,
  },
  props: {
    modelValue: {
      type: Object as PropType<EditStepData>,
      required: true,
      default: () => ({}) as EditStepData,
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
  emits: ["cancel", "save", "update:modelValue", "switch-step-type"],
  data() {
    return {
      editModel: {} as EditStepData,
      stepName: "",
      conditionSets: [createEmptyConditionSet()] as ConditionSet[],
    };
  },
  watch: {
    modelValue: {
      handler(val) {
        if (val && Object.keys(val).length > 0) {
          this.editModel = cloneDeep(val);
          this.stepName = val.description || "";
          this.conditionSets = val.config?.conditionSets || [createEmptyConditionSet()];
        }
      },
      immediate: true,
      deep: true,
    },
  },
  mounted() {
    this.editModel = cloneDeep(this.modelValue);
    this.stepName = this.modelValue.description || "";
    this.conditionSets = this.modelValue.config?.conditionSets || [createEmptyConditionSet()];
  },
  computed: {
    cardTitle(): string {
      return this.serviceName === ServiceType.WorkflowStep
        ? this.$t("editConditionalStep.titleWorkflow")
        : this.$t("editConditionalStep.title");
    },
    cardDescription(): string {
      return this.serviceName === ServiceType.WorkflowStep
        ? this.$t("editConditionalStep.descriptionWorkflow")
        : this.$t("editConditionalStep.description");
    },
    canAddConditionSet(): boolean {
      return this.conditionSets.length < MAX_CONDITION_SETS;
    },
    fieldOptions(): FieldOption[] {
      const nodeAttrs = contextVariables().node || [];
      return nodeAttrs.map((attr) => ({
        value: `node.${attr.name}`,
        label: `${attr.title} [node.${attr.name}]`,
      }));
    },
    operatorOptions(): OperatorOption[] {
      return [
        { label: this.$t("Workflow.conditional.operator.equals"), value: "equals" },
        { label: this.$t("Workflow.conditional.operator.notEquals"), value: "notEquals" },
        { label: this.$t("Workflow.conditional.operator.contains"), value: "contains" },
        { label: this.$t("Workflow.conditional.operator.notContains"), value: "notContains" },
        { label: this.$t("Workflow.conditional.operator.regex"), value: "regex" },
      ];
    },
    autocompleteSuggestions(): ContextVariable[] {
      const jobVars = (contextVariables().job || []).map((v) => ({
        ...v,
        name: `\${job.${v.name}}`,
      }));
      const optionVars = this.extraAutocompleteVars
        .filter((v) => v.type === "option")
        .map((v) => ({
          ...v,
          name: `\${option.${v.name}}`,
        }));
      return [...jobVars, ...optionVars];
    },
  },
  methods: {
    canAddCondition(setIndex: number): boolean {
      return this.conditionSets[setIndex].conditions.length < MAX_CONDITIONS_PER_SET;
    },
    addCondition(setIndex: number) {
      if (this.canAddCondition(setIndex)) {
        this.conditionSets[setIndex].conditions.push(createEmptyCondition());
      }
    },
    removeCondition(setIndex: number, conditionIndex: number) {
      const set = this.conditionSets[setIndex];
      if (set.conditions.length > 1) {
        set.conditions.splice(conditionIndex, 1);
      } else if (this.conditionSets.length > 1) {
        this.conditionSets.splice(setIndex, 1);
      }
    },
    updateCondition(setIndex: number, conditionIndex: number, updatedCondition: Condition) {
      this.conditionSets[setIndex].conditions[conditionIndex] = updatedCondition;
    },
    addConditionSet() {
      if (this.canAddConditionSet) {
        this.conditionSets.push(createEmptyConditionSet());
      }
    },
    handleSave() {
      const updatedModel = {
        ...this.editModel,
        description: this.stepName,
        config: {
          ...this.editModel.config,
          conditionSets: this.conditionSets,
        },
      };
      this.$emit("update:modelValue", updatedModel);
      this.$emit("save");
    },
    handleCancel() {
      this.$emit("cancel");
    },
    handleAddConditionStep() {
      // Add condition step logic - to be implemented
    },
    handleSwitchStepType() {
      this.$emit("switch-step-type");
    },
  },
});
</script>

<style lang="scss">
.edit-conditional-step-card {
  display: flex;
  position: relative;
  width: 100%;

  .p-card {
    flex: 1;
    box-shadow: none;
    border: 1px solid var(--colors-gray-200);
    border-radius: var(--radii-md);
    overflow: hidden;

    .p-card-body {
      padding: 0;
    }

    .p-card-header {
      padding-bottom: 0;
    }

    .p-card-content {
      display: flex;
      flex-direction: column;
      gap: var(--sizes-6);
      padding: 24px;
    }

    .p-card-footer {
      padding: 0 var(--sizes-6) var(--sizes-6);
    }
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    padding: 24px 24px 0 24px;

    .title-section {
      display: flex;
      flex-direction: column;
      gap: var(--sizes-1);
    }

    .title-with-icon {
      display: flex;
      align-items: center;
      gap: var(--sizes-1);
    }

    .condition-icon {
      width: 24px;
      height: 24px;
      object-fit: contain;
    }

    .card-title {
      margin: 0;
      font-weight: var(--fontWeights-semibold);
      color: var(--colors-gray-800);
    }

    .card-description {
      margin: 0;
      margin-top: var(--sizes-2);
    }

    .close-button {
      flex-shrink: 0;
    }
  }

  .step-name-section {
    display: flex;
    flex-direction: column;
    gap: var(--sizes-1);

    .form-label {
      margin: 0;
    }

    .helper-text {
      margin: 0;
      color: var(--colors-gray-800);
    }

    .step-name-input {
      width: 100%;
    }
  }

  .condition-section {
    display: flex;
    flex-direction: column;
    gap: var(--sizes-4);

    .section-title {
      margin: 0;
      color: var(--colors-gray-800);
    }

    .section-description {
      margin: 0;
    }
  }

  .condition-set-container {
    display: flex;
    flex-direction: column;
    gap: var(--sizes-4);
  }

  .or-separator {
    display: flex;
    align-items: center;

    .or-label {
      font-family: Inter, var(--fonts-body);
      font-size: 16px;
      font-weight: var(--fontWeights-medium);
      color: var(--colors-gray-500);
    }
  }

  .condition-set {
    display: flex;
    flex-direction: column;
    gap: 12px;

    .condition-set-title {
      margin: 0;
      font-family: Inter, var(--fonts-body);
      font-size: 16px;
      font-weight: var(--fontWeights-medium);
      color: var(--colors-gray-800);
    }
  }

  .conditions-list {
    display: flex;
    flex-direction: column;
    gap: 0;
  }

  .conditions-connector {
    display: flex;
    flex-direction: column;
    gap: var(--sizes-3);
    position: relative;
    transition: padding-left 0.2s ease;

    &.has-multiple {
      padding-left: 22px;
      &::before {
        content: "";
        position: absolute;
        left: 0;
        top: 40px;
        bottom: 20px;
        width: 14px;
        border-left: 1px solid var(--colors-gray-300-original);
        border-top: 1px solid var(--colors-gray-300-original);
        border-bottom: 1px solid var(--colors-gray-300-original);
        border-right: none;
        border-radius: 6px 0 0 6px;
      }
    }
  }

  .and-separator {
    display: flex;
    align-items: center;
    position: relative;

    .and-label {
      font-family: Inter, var(--fonts-body);
      font-size: 14px;
      font-weight: var(--fontWeights-semibold);
      color: var(--colors-gray-600);
    }
  }

  .condition-actions {
    display: flex;
    align-items: center;

    &.has-multiple {
      padding-left: 22px;
    }
  }

  .condition-set-actions {
    display: flex;
    align-items: center;
    margin-top: var(--sizes-2);
  }

  .btn-add-link {
    display: inline-flex;
    align-items: center;
    gap: var(--sizes-1);
    background: var(--colors-blue-50, #f5f9ff);
    color: var(--colors-blue-600, #0052cc);
    border: none;
    border-radius: var(--radii-md);
    padding: 5px 9px;
    font-family: Inter, var(--fonts-body);
    font-size: 12px;
    font-weight: var(--fontWeights-medium);
    line-height: 16px;
    cursor: pointer;
    transition: background-color 0.2s;

    &:hover {
      background: var(--colors-blue-100);
    }

    i {
      font-size: 12px;
    }
  }

  .btn-add-condition-set,
  .btn-add-condition-step {
    align-self: flex-start;
    padding: 5px 9px;
    font-size: 12px;
    border-color: var(--colors-gray-600);
    color: var(--colors-gray-800);

    &:hover {
      background: var(--colors-gray-100);
      border-color: var(--colors-gray-800);
    }

    :deep(.pi) {
      font-size: 12px;
    }
  }

  .divider {
    height: 1px;
    background: var(--colors-gray-200);
    margin: var(--sizes-2) 0;
  }

  .steps-section {
    display: flex;
    flex-direction: column;
    gap: var(--sizes-4);

    .section-title {
      margin: 0;
      color: var(--colors-black);
    }
  }

  .card-footer {
    display: flex;
    justify-content: flex-end;
    gap: var(--sizes-3);
    padding-top: var(--sizes-4);

    .btn-cancel {
      border-color: var(--colors-gray-800);
      color: var(--colors-gray-800);
    }

    .btn-save {
      background: none;
      border-color: var(--colors-blue-600);
      color: var(--colors-blue-600);

      &:hover {
        background: var(--colors-blue-50, #f5f9ff);
      }
    }
  }
}
</style>
