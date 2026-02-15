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

          <ConditionsEditor
            ref="conditionsEditor"
            v-model="conditionSets"
            :service-name="serviceName"
            :extra-autocomplete-vars="extraAutocompleteVars"
            :depth="depth"
            @switch-step-type="handleSwitchStepType"
          />
        </div>

        <div class="divider"></div>

        <div class="steps-section">
          <h3 class="text-heading--md section-title">{{ $t("editConditionalStep.setSteps") }}</h3>
          <InnerStepList
            v-model="innerCommands"
            :target-service="serviceName"
            :depth="depth + 1"
            :extra-autocomplete-vars="extraAutocompleteVars"
            @update:editing="isEditingInnerStep = $event"
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
            :disabled="isSaveDisabled"
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
import ConditionsEditor from "./ConditionsEditor.vue";
import InnerStepList from "./InnerStepList.vue";
import { ServiceType } from "@/library/stores/Plugins";
import { cloneDeep } from "lodash";
import {
  type ConditionSet,
  createEmptyConditionSet,
} from "./types/conditionalStepTypes";
import type { EditStepData } from "./types/workflowTypes";
import type { ContextVariable } from "@/library/stores/contextVariables";

export default defineComponent({
  name: "EditConditionalStepCard",
  components: {
    Card,
    PtInput,
    PtButton,
    ConditionsEditor,
    InnerStepList,
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
    depth: {
      type: Number,
      default: 0,
    },
  },
  emits: ["cancel", "save", "update:modelValue", "switch-step-type"],
  data() {
    return {
      editModel: {} as EditStepData,
      stepName: "",
      conditionSets: [createEmptyConditionSet()] as ConditionSet[],
      innerCommands: [] as EditStepData[],
      isEditingInnerStep: false,
    };
  },
  watch: {
    modelValue: {
      handler(val) {
        if (val && Object.keys(val).length > 0) {
          this.editModel = cloneDeep(val);
          this.stepName = val.description || "";
          this.conditionSets = val.config?.conditionSets || [createEmptyConditionSet()];
          this.innerCommands = val.config?.commands || [];
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
    this.innerCommands = this.modelValue.config?.commands || [];
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
    isSaveDisabled(): boolean {
      return this.innerCommands.length === 0 || this.isEditingInnerStep;
    },
  },
  methods: {
    handleSave() {
      // Delegate conditions validation to ConditionsEditor
      const conditionsEditor = this.$refs.conditionsEditor as InstanceType<typeof ConditionsEditor>;
      if (conditionsEditor && !conditionsEditor.validate()) {
        return;
      }

      const updatedModel = {
        ...this.editModel,
        description: this.stepName,
        config: {
          ...this.editModel.config,
          conditionSets: this.conditionSets,
          commands: this.innerCommands,
        },
      };
      this.$emit("update:modelValue", updatedModel);
      this.$emit("save");
    },
    handleCancel() {
      this.$emit("cancel");
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
      margin: 0 0 8px 0;
      color: var(--colors-gray-800);
    }

    .section-description {
      margin: 0 0 12px 0;
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
