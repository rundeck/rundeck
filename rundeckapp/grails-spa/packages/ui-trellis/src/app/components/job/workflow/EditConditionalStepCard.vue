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
              <h2 class="text-heading--lg card-title">{{ $t("editConditionalStep.title") }}</h2>
            </div>
            <p class="text-body text-body--secondary card-description">
              {{ $t("editConditionalStep.description") }}
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
          <InputText
            v-model="stepName"
            :placeholder="$t('editConditionalStep.stepNamePlaceholder')"
            class="step-name-input"
          />
        </div>

        <div class="condition-section">
          <h3 class="text-heading--md section-title">{{ $t("editConditionalStep.defineCondition") }}</h3>
          <p class="text-body text-body--secondary section-description">
            {{ $t("editConditionalStep.defineConditionHelper") }}
          </p>

          <div class="criteria-fields">
            <div class="field-group field-column">
              <label class="text-heading--sm form-label">
                {{ $t("editConditionalStep.field") }} <span class="required-indicator">*</span>
              </label>
              <PtSelect
                v-model="selectedField"
                :options="fieldOptions"
                :placeholder="$t('editConditionalStep.selectPlaceholder')"
                class="field-select"
              />
            </div>

            <div class="field-group operator-column">
              <label class="text-heading--sm form-label">{{ $t("editConditionalStep.operator") }}</label>
              <PtSelect
                v-model="selectedOperator"
                :options="operatorOptions"
                option-label="label"
                option-value="value"
                :placeholder="$t('editConditionalStep.operatorPlaceholder')"
                class="operator-select"
              />
            </div>

            <div class="field-group value-column">
              <label class="text-heading--sm form-label">
                {{ $t("editConditionalStep.value") }} <span class="required-indicator">*</span>
              </label>
              <InputText
                v-model="conditionValue"
                :placeholder="$t('editConditionalStep.valuePlaceholder')"
                class="value-input"
              />
            </div>
          </div>

          <div class="condition-actions">
            <button type="button" class="btn-add-link" @click="handleAddCondition">
              <i class="fas fa-plus"></i>
              <span>{{ $t("editConditionalStep.add") }}</span>
            </button>
          </div>

          <div class="condition-set-actions">
            <button type="button" class="btn-outline-secondary" @click="handleAddConditionSet">
              <i class="fas fa-plus"></i>
              <span>{{ $t("editConditionalStep.addConditionSet") }}</span>
            </button>
          </div>
        </div>

        <div class="divider"></div>

        <div class="steps-section">
          <h3 class="text-heading--md section-title">{{ $t("editConditionalStep.setSteps") }}</h3>
          <button type="button" class="btn-outline-secondary" @click="handleAddConditionStep">
            <i class="fas fa-plus"></i>
            <span>{{ $t("editConditionalStep.addConditionStep") }}</span>
          </button>
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
import { defineComponent } from "vue";
import Card from "primevue/card";
import InputText from "primevue/inputtext";
import PtButton from "@/library/components/primeVue/PtButton/PtButton.vue";
import PtSelect from "@/library/components/primeVue/PtSelect/PtSelect.vue";

export default defineComponent({
  name: "EditConditionalStepCard",
  components: {
    Card,
    InputText,
    PtButton,
    PtSelect,
  },
  emits: ["cancel", "save"],
  data() {
    return {
      stepName: "",
      selectedField: null as string | null,
      selectedOperator: "equal",
      conditionValue: "",
      fieldOptions: [] as string[],
      operatorOptions: [
        { label: "Equal", value: "equal" },
        { label: "Not Equal", value: "notEqual" },
        { label: "Contains", value: "contains" },
        { label: "Starts With", value: "startsWith" },
        { label: "Ends With", value: "endsWith" },
      ],
    };
  },
  methods: {
    handleSave() {
      this.$emit("save");
    },
    handleCancel() {
      this.$emit("cancel");
    },
    handleAddCondition() {
      // Add condition logic
    },
    handleAddConditionSet() {
      // Add condition set logic
    },
    handleAddConditionStep() {
      // Add condition step logic
    },
  },
});
</script>

<style lang="scss">
@import "@/library/theme/typography.css";

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
      padding: var(--sizes-6);
    }

    .p-card-footer {
      padding: 0 var(--sizes-6) var(--sizes-6);
    }
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;

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

  .criteria-fields {
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
    }
  }

  .condition-actions {
    display: flex;
    align-items: center;
  }

  .condition-set-actions {
    display: flex;
    align-items: center;
  }

  .btn-add-link {
    display: inline-flex;
    align-items: center;
    gap: var(--sizes-1);
    background: var(--colors-blue-100);
    color: var(--colors-blue-500);
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
      background: var(--colors-blue-200);
    }

    i {
      font-size: 12px;
    }
  }

  .btn-outline-secondary {
    display: inline-flex;
    align-items: center;
    gap: var(--sizes-1);
    background: var(--colors-white);
    color: var(--colors-gray-800);
    border: 1px solid var(--colors-gray-600);
    border-radius: var(--radii-md);
    padding: 5px 9px;
    font-family: Inter, var(--fonts-body);
    font-size: 12px;
    font-weight: var(--fontWeights-medium);
    line-height: 16px;
    cursor: pointer;
    transition: background-color 0.2s, border-color 0.2s;

    &:hover {
      background: var(--colors-gray-100);
      border-color: var(--colors-gray-800);
    }

    i {
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
      border-color: var(--colors-blue-600);
      color: var(--colors-blue-600);

      &:hover {
        background: var(--colors-blue-50, #f5f9ff);
      }
    }
  }
}
</style>
