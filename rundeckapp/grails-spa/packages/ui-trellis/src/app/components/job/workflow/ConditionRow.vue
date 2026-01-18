<template>
  <div class="condition-row">
    <div class="field-group field-column">
      <label v-if="showLabels" class="text-heading--sm form-label">
        {{ $t("editConditionalStep.field") }} <span class="required-indicator">*</span>
      </label>
      <PtSelect
        :modelValue="condition.field"
        :options="fieldOptions"
        option-label="label"
        option-value="value"
        :placeholder="$t('editConditionalStep.selectPlaceholder')"
        class="field-select"
        @update:modelValue="updateField"
      />
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
      <InputText
        :modelValue="condition.value"
        :placeholder="$t('editConditionalStep.valuePlaceholder')"
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
import InputText from "primevue/inputtext";
import PtButton from "@/library/components/primeVue/PtButton/PtButton.vue";
import PtSelect from "@/library/components/primeVue/PtSelect/PtSelect.vue";
import type { Condition, OperatorOption, FieldOption } from "./types/conditionalStepTypes";

export default defineComponent({
  name: "ConditionRow",
  components: {
    InputText,
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
  },
  emits: ["update:condition", "delete"],
  methods: {
    updateField(value: string | null) {
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
    max-width: 474px;
  }

  .operator-column {
    width: 175px;
    flex-shrink: 0;
  }

  .value-column {
    flex: 2;
    max-width: 474px;

    .value-input {
      width: 100%;
    }
  }

  .delete-button {
    flex-shrink: 0;
    padding: 12px;
    border-color: var(--colors-gray-600);
    color: var(--colors-gray-800);

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
