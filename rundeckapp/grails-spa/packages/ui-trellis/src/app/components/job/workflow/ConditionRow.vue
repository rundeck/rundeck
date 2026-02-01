<template>
  <div class="condition-row">
    <div class="field-group field-column">
      <label v-if="showLabels" class="text-heading--sm form-label">
        {{ $t("editConditionalStep.field") }} <span class="required-indicator">*</span>
      </label>
      <PtSelect
        :modelValue="condition.field"
        :options="fieldOptionsWithNote"
        option-label="label"
        option-value="value"
        :placeholder="$t('editConditionalStep.selectPlaceholder')"
        class="field-select"
        @update:modelValue="updateField"
      >
        <template #option="slotProps">
          <div
            v-if="slotProps.option.isNote"
            class="note-option"
          >
            <span class="note-text">{{ $t("editConditionalStep.switchStepTypeNote") }}</span>
            <button
              type="button"
              class="note-link"
              @click.stop="handleNoteClick"
            >
              {{ $t("editConditionalStep.switchStepTypeLink", { stepType: oppositeStepType }) }}
            </button>
          </div>
          <div v-else class="regular-option">{{ slotProps.option.label }}</div>
        </template>
      </PtSelect>
    </div>

    <div class="field-group operator-column">
      <label v-if="showLabels" class="text-heading--sm form-label">
        {{ $t("editConditionalStep.operator") }}
      </label>
      <PtSelect
        :modelValue="condition.operator"
        :options="operatorOptions"
        option-label="label"
        option-value="value"
        :placeholder="$t('editConditionalStep.operatorPlaceholder')"
        class="operator-select"
        @update:modelValue="updateOperator"
      />
    </div>

    <div class="field-group value-column">
      <label v-if="showLabels" class="text-heading--sm form-label">
        {{ $t("editConditionalStep.value") }} <span class="required-indicator">*</span>
      </label>
      <PtAutoComplete
        :modelValue="condition.value"
        :suggestions="hasSuggestions ? suggestions : undefined"
        :placeholder="$t('editConditionalStep.valuePlaceholder')"
        :tab-mode="tabMode"
        :tabs="tabs"
        class="value-input"
        @update:modelValue="updateValue"
      />
    </div>

    <PtButton
      v-if="showDeleteButton"
      outlined
      severity="secondary"
      icon="pi pi-trash"
      class="delete-button"
      @click="handleDelete"
    />
  </div>
</template>

<script lang="ts">
import { defineComponent, type PropType } from "vue";
import PtAutoComplete from "@/library/components/primeVue/PtAutoComplete/PtAutoComplete.vue";
import PtButton from "@/library/components/primeVue/PtButton/PtButton.vue";
import PtSelect from "@/library/components/primeVue/PtSelect/PtSelect.vue";
import type { Condition, OperatorOption, FieldOption } from "./types/conditionalStepTypes";
import type { ContextVariable } from "@/library/stores/contextVariables";
import type { TabConfig } from "@/library/components/primeVue/PtAutoComplete/PtAutoCompleteTypes";

export default defineComponent({
  name: "ConditionRow",
  components: {
    PtAutoComplete,
    PtButton,
    PtSelect,
  },
  props: {
    condition: {
      type: Object as PropType<Condition>,
      required: true,
    },
    fieldOptions: {
      type: Array as PropType<FieldOption[]>,
      default: () => [],
    },
    operatorOptions: {
      type: Array as PropType<OperatorOption[]>,
      required: true,
    },
    showLabels: {
      type: Boolean,
      default: true,
    },
    showDeleteButton: {
      type: Boolean,
      default: true,
    },
    serviceName: {
      type: String,
      required: true,
    },
    suggestions: {
      type: Array as PropType<ContextVariable[]>,
      default: () => [],
    },
    tabMode: {
      type: Boolean,
      default: false,
    },
  },
  emits: ["update:condition", "delete", "switch-step-type"],
  computed: {
    oppositeStepType(): string {
      // If serviceName is "WorkflowStep", return "node", otherwise return "workflow"
      return this.serviceName === "WorkflowStep" ? "node" : "workflow";
    },
    fieldOptionsWithNote(): FieldOption[] {
      const noteOption: FieldOption = {
        value: "__NOTE__",
        label: "",
        isNote: true,
      };
      return [noteOption, ...this.fieldOptions];
    },
    hasSuggestions(): boolean {
      return this.suggestions && this.suggestions.length > 0;
    },
    tabs(): TabConfig[] | undefined {
      if (!this.tabMode) {
        return undefined;
      }

      return [
        {
          label: this.$t("editConditionalStep.tabJob") || "Job",
          filter: (suggestion: ContextVariable) => suggestion.type === "job",
          getCount: (suggestions: ContextVariable[]) =>
            suggestions.filter((s) => s.type === "job").length,
        },
        {
          label: this.$t("editConditionalStep.tabOptions") || "Options",
          filter: (suggestion: ContextVariable) => suggestion.type === "option",
          getCount: (suggestions: ContextVariable[]) =>
            suggestions.filter((s) => s.type === "option").length,
        },
      ];
    },
  },
  methods: {
    updateField(value: string | null) {
      // Don't allow selecting the note option
      if (value === "__NOTE__") {
        return;
      }
      this.$emit("update:condition", {
        ...this.condition,
        field: value,
      });
    },
    updateOperator(value: string) {
      this.$emit("update:condition", {
        ...this.condition,
        operator: value,
      });
    },
    updateValue(value: string) {
      this.$emit("update:condition", {
        ...this.condition,
        value: value,
      });
    },
    handleDelete() {
      this.$emit("delete", this.condition.id);
    },
    handleNoteClick() {
      this.$emit("switch-step-type");
    },
  },
});
</script>

<style lang="scss" scoped>
.condition-row {
  display: flex;
  gap: var(--sizes-3);
  align-items: flex-end;

  .field-group {
    display: flex;
    flex-direction: column;
    gap: var(--sizes-1);

    .form-label {
      margin: 0;
      color: var(--colors-gray-800);
    }

    .required-indicator {
      color: var(--colors-red-500);
    }
  }

  .field-column {
    flex: 2;
    max-width: 520px;
  }

  .operator-column {
    width: 175px;
    flex-shrink: 0;
  }

  .value-column {
    flex: 2 1 0;
    min-width: 200px;

    .value-input {
      width: 100%;
    }
  }

  .delete-button {
    flex-shrink: 0;
    width: 38px;
    height: 38px;
    padding: 0;
    border-color: var(--colors-gray-600);
    color: var(--colors-gray-800);
    display: flex;
    align-items: center;
    justify-content: center;

    &:hover {
      background: var(--colors-gray-100);
      border-color: var(--colors-gray-800);
    }

    :deep(.pi) {
      font-size: 14px;
    }
  }
}
</style>
<style>
.note-option {
  padding: 0;
  cursor: default;
  font-size: 14px;
  color: var(--colors-gray-800);
  line-height: 1.5;
  pointer-events: auto;
}

.note-text {
  color: var(--colors-gray-800);
}

.note-link {
  background: none;
  border: none;
  padding: 0;
  margin: 0;
  font-family: inherit;
  font-size: inherit;
  line-height: inherit;
  color: var(--colors-blue-600);
  text-decoration: underline;
  cursor: pointer;
  pointer-events: auto;
  display: inline;
}

.note-link:hover {
  color: var(--colors-blue-700);
  text-decoration: underline;
}

.note-link:focus {
  outline: 1px solid var(--colors-blue-600);
  outline-offset: 2px;
  border-radius: 2px;
}

.note-link:active {
  color: var(--colors-blue-700);
}
</style>
