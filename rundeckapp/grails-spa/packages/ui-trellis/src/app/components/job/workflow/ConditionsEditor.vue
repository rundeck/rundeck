<template>
  <div class="conditions-editor">
    <div class="condition-section">
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
                  :field-error="conditionErrors[condition.id]?.field ? $t(conditionErrors[condition.id].field) : undefined"
                  :value-error="conditionErrors[condition.id]?.value ? $t(conditionErrors[condition.id].value) : undefined"
                  :depth="depth"
                  @update:condition="(payload) => updateCondition(setIndex, condIndex, payload.condition, payload.fieldName)"
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
          :label="addConditionSetLabel"
          class="btn-add-condition-set"
          @click="addConditionSet"
        />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, type PropType } from "vue";
import PtButton from "@/library/components/primeVue/PtButton/PtButton.vue";
import ConditionRow from "./ConditionRow.vue";
import { contextVariables, type ContextVariable } from "@/library/stores/contextVariables";
import { ServiceType } from "@/library/stores/Plugins";
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

export default defineComponent({
  name: "ConditionsEditor",
  components: {
    PtButton,
    ConditionRow,
  },
  props: {
    modelValue: {
      type: Array as PropType<ConditionSet[]>,
      required: true,
      default: () => [createEmptyConditionSet()],
    },
    serviceName: {
      type: String,
      required: true,
    },
    extraAutocompleteVars: {
      type: Array as PropType<ContextVariable[]>,
      required: false,
      default: () => [],
    },
    depth: {
      type: Number,
      default: 0,
    },
    validation: {
      type: Object as PropType<{ valid: boolean; errors: Record<string, any> }>,
      required: false,
      default: () => ({ valid: true, errors: {} }),
    },
  },
  emits: ["update:modelValue", "switch-step-type"],
  data() {
    return {
      conditionSets: [] as ConditionSet[],
    };
  },
  watch: {
    modelValue: {
      handler(val) {
        if (val && val.length > 0) {
          this.conditionSets = val;
        }
      },
      immediate: true,
      deep: true,
    },
  },
  mounted() {
    this.conditionSets = this.modelValue.length > 0
      ? this.modelValue
      : [createEmptyConditionSet()];
  },
  computed: {
    canAddConditionSet(): boolean {
      return this.conditionSets.length < MAX_CONDITION_SETS;
    },
    addConditionSetLabel(): string {
      return this.depth >= 1
        ? this.$t("editConditionalStep.addNestedConditionSet")
        : this.$t("editConditionalStep.addConditionSet");
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
    conditionErrors(): Record<string, { field?: string; value?: string }> {
      return this.validation?.errors?.conditions || {};
    },
  },
  methods: {
    canAddCondition(setIndex: number): boolean {
      return this.conditionSets[setIndex].conditions.length < MAX_CONDITIONS_PER_SET;
    },
    addCondition(setIndex: number) {
      if (this.canAddCondition(setIndex)) {
        const newCondition = createEmptyCondition();
        this.conditionSets[setIndex].conditions.push(newCondition);
        this.emitUpdate();
      }
    },
    removeCondition(setIndex: number, conditionIndex: number) {
      const set = this.conditionSets[setIndex];
      if (set.conditions.length > 1) {
        set.conditions.splice(conditionIndex, 1);
      } else if (this.conditionSets.length > 1) {
        this.conditionSets.splice(setIndex, 1);
      }
      this.emitUpdate();
    },
    updateCondition(
      setIndex: number,
      conditionIndex: number,
      updatedCondition: Condition,
      fieldName?: "field" | "value" | "operator",
    ) {
      this.conditionSets[setIndex].conditions[conditionIndex] = updatedCondition;
      this.emitUpdate();
    },
    addConditionSet() {
      if (this.canAddConditionSet) {
        const newSet = createEmptyConditionSet();
        this.conditionSets.push(newSet);
        this.emitUpdate();
      }
    },
    emitUpdate() {
      this.$emit("update:modelValue", this.conditionSets);
    },
    handleSwitchStepType() {
      this.$emit("switch-step-type");
    },
  },
});
</script>

<style lang="scss">
.conditions-editor {
  .condition-section {
    display: flex;
    flex-direction: column;
    gap: var(--sizes-4);
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
        margin-bottom: 20px;
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

  .btn-add-condition-set {
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
}
</style>
