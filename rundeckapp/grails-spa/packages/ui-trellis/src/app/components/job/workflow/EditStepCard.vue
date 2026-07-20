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
          :model-value="editModel"
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
          @update:model-value="onPluginConfigUpdate"
        />
      </div>

      <div v-else-if="loading" class="loading-container" data-testid="loading-container">
        <i class="fas fa-spinner fa-spin"></i>
        <span>{{ $t("loading.text") }}</span>
      </div>

      <!--
        Error handler is allowed on job-reference steps (matches the legacy
        GSP: only log filters are excluded for job refs, not error handlers)
        -- so this section renders for both isJobRef and provider-loaded
        steps. Gating on (provider || isJobRef) -- rather than just !loading
        -- avoids rendering on the very first synchronous tick, before
        mounted() has populated editModel from modelValue.
      -->
      <ConfigSection
        v-if="!loading && (provider || isJobRef)"
        data-testid="error-handler-section"
        :is-edit-view="true"
        :title="$t('Workflow.stepErrorHandler')"
        :tooltip="$t('Workflow.stepErrorHandler.description')"
        :model-value="errorHandlerValue"
        :hide-when-single="true"
        :hide-icon="true"
        @add-element="handleAddErrorHandler"
        @edit-element="handleEditErrorHandler"
        @remove-element="handleRemoveErrorHandler"
      >
        <template v-if="showInlineErrorHandlerForm" #extra></template>
        <template #content>
          <inline-plugin-config-form
            v-if="showInlineErrorHandlerForm"
            ref="inlineErrorHandlerForm"
            :model-value="editModel.errorhandler"
            :service-name="errorHandlerServiceName"
            :validation="errorHandlerValidation"
            :extra-autocomplete-vars="extraAutocompleteVars"
            :show-buttons="false"
            @update:model-value="onErrorHandlerFormUpdate"
          >
            <template #extra>
              <div class="presentation checkbox">
                <input
                  id="editStepKeepgoingOnSuccess"
                  v-model="editModel.errorhandler.keepgoingOnSuccess"
                  type="checkbox"
                />
                <label for="editStepKeepgoingOnSuccess">
                  {{ $t("Workflow.stepErrorHandler.keepgoingOnSuccess.label") }}
                  <span>
                    {{
                      $t(
                        "Workflow.stepErrorHandler.keepgoingOnSuccess.description",
                      )
                    }}
                  </span>
                </label>
              </div>
            </template>
          </inline-plugin-config-form>
        </template>
      </ConfigSection>

      <!-- Log filters are excluded for job-reference steps, matching the legacy GSP. -->
      <ConfigSection
        v-if="!isJobRef && !loading && provider"
        data-testid="log-filter-section"
        :is-edit-view="true"
        :title="$t('Workflow.logFilters')"
        :tooltip="$t('Workflow.logFilters.description')"
        :model-value="logFiltersValue"
        @add-element="handleAddLogFilter"
        @edit-element="handleEditLogFilter"
        @remove-element="handleRemoveLogFilter"
      />
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
import ConfigSection from "../../../../library/components/primeVue/StepCards/ConfigSection.vue";
import inlinePluginConfigForm from "../../../../library/components/plugins/InlinePluginConfigForm.vue";
import { PluginConfig } from "../../../../library/interfaces/PluginConfig";
import { getServiceProviderDescription } from "../../../../library/modules/pluginService";
import { ServiceType } from "../../../../library/stores/Plugins";
import { ContextVariable } from "../../../../library/stores/contextVariables";
import { cloneDeep, merge } from "lodash";
import { getRundeckContext } from "../../../../library";
import type { EditStepData } from "./types/workflowTypes";
import JobRefFormFields from "./JobRefFormFields.vue";
import VueScrollTo from "vue-scrollto";
import { resetValidation, createJobRefDefinition, type PluginDetails } from "./stepEditorUtils";

const rundeckContext = getRundeckContext();

export default defineComponent({
  name: "EditStepCard",
  components: {
    BaseStepCard,
    pluginConfig,
    PtButton,
    ConfigSection,
    inlinePluginConfigForm,
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
  emits: [
    "cancel",
    "save",
    "update:modelValue",
    "add-error-handler",
    "edit-error-handler",
    "remove-error-handler",
    "add-log-filter",
    "edit-log-filter",
    "remove-log-filter",
  ],
  data() {
    return {
      contentExpanded: true,
      editModel: {} as EditStepData,
      stepDescription: "",
      provider: null as PluginDetails | null,
      loading: false,
      pluginConfigMode: "edit",
      validationErrors: resetValidation(),
      jobRefDefaults: {
        description: "",
        jobref: createJobRefDefinition(rundeckContext.projectName),
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
    errorHandlerValue(): object[] {
      const handler =
        this.editModel?.errorhandler ?? this.modelValue?.errorhandler;
      return handler ? [handler] : [];
    },
    logFiltersValue(): object[] {
      return this.editModel?.filters ?? this.modelValue?.filters ?? [];
    },
    showInlineErrorHandlerForm(): boolean {
      return Boolean(
        this.editModel?.errorhandler?.type ??
          this.modelValue?.errorhandler?.type,
      );
    },
    errorHandlerServiceName(): string {
      const handler =
        this.editModel?.errorhandler ?? this.modelValue?.errorhandler;
      if (!handler) {
        return this.serviceName;
      }
      return handler.nodeStep
        ? ServiceType.WorkflowNodeStep
        : ServiceType.WorkflowStep;
    },
    errorHandlerValidation() {
      const handlerErrors = this.validation?.errors?.errorhandler;
      if (handlerErrors && typeof handlerErrors === "object") {
        return { valid: false, errors: handlerErrors };
      }
      return { valid: true, errors: {} };
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
      if (this.isJobRef) {
        const saveData = {
          ...cloneDeep(this.modelValue),
          description: this.stepDescription,
          type: this.editModel.type,
          nodeStep: this.editModel.nodeStep,
          config: this.editModel.config ?? {},
          jobref: cloneDeep(this.editModel.jobref),
        };
        this.$emit("update:modelValue", saveData);
        this.$emit("save");
        return;
      }

      const saveData: EditStepData = {
        ...cloneDeep(this.modelValue),
        description: this.stepDescription,
        type: this.editModel.type,
        config: this.editModel.config ?? {},
      };

      if (this.showInlineErrorHandlerForm) {
        const inlineForm = this.$refs.inlineErrorHandlerForm as {
          getFormData?: () => Record<string, unknown>;
        } | null;
        if (inlineForm?.getFormData) {
          const formData = cloneDeep(inlineForm.getFormData()) as Record<
            string,
            unknown
          >;
          // Only the plugin fields (type/config) come from the inline form.
          // keepgoingOnSuccess/nodeStep/id stay owned by editModel.errorhandler
          // (keepgoingOnSuccess is bound to the checkbox), so they are preserved.
          this.editModel = {
            ...this.editModel,
            errorhandler: {
              ...this.editModel.errorhandler,
              type: formData.type,
              config: formData.config,
            },
          };
        }
      }

      if (this.editModel.errorhandler) {
        saveData.errorhandler = cloneDeep(this.editModel.errorhandler);
      } else {
        delete saveData.errorhandler;
      }

      if (this.editModel.filters !== undefined) {
        saveData.filters = cloneDeep(this.editModel.filters);
      }

      this.$emit("update:modelValue", saveData);
      this.$emit("save");
    },
    handleCancel() {
      this.$emit("cancel");
    },
    handleAddErrorHandler() {
      this.$emit("add-error-handler");
    },
    handleEditErrorHandler(_element: unknown) {
      if (this.showInlineErrorHandlerForm) {
        return;
      }
      this.$emit("edit-error-handler", _element);
    },
    handleRemoveErrorHandler() {
      this.$emit("remove-error-handler");
    },
    onErrorHandlerFormUpdate(updated: PluginConfig) {
      // The inline form owns only the plugin fields (type/config). Merge them in
      // without replacing the whole handler, so checkbox-owned fields
      // (keepgoingOnSuccess) and nodeStep/id are preserved on every edit.
      this.editModel = {
        ...this.editModel,
        errorhandler: {
          ...this.editModel.errorhandler,
          type: updated?.type,
          config: updated?.config,
        },
      };
    },
    handleAddLogFilter() {
      this.$emit("add-log-filter");
    },
    handleEditLogFilter(element: PluginConfig, index: number) {
      this.$emit("edit-log-filter", { element, index });
    },
    handleRemoveLogFilter(index: number) {
      this.$emit("remove-log-filter", index);
    },
    onPluginConfigUpdate(updated: EditStepData) {
      // ConfigSection chips use modelValue (errorhandler/filters). Only merge plugin fields from plugin-config;
      // do not clobber handler/filters on editModel when the child emits a narrowed payload.
      const { errorhandler: _eh, filters: _filters, ...pluginFields } = updated;
      this.editModel = {
        ...this.editModel,
        ...pluginFields,
      };
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
  watch: {
    async modelValue(newVal) {
      this.stepDescription = newVal.description || "";

      if (newVal.jobref) {
        this.editModel = merge(cloneDeep(this.jobRefDefaults), newVal);
      } else {
        const { description, ...rest } = newVal;
        this.editModel = cloneDeep(rest);
      }

      await this.loadProvider();
    },
    async pluginDetails() {
      await this.loadProvider();
    },
    "modelValue.errorhandler": {
      handler(val) {
        if (val?.type && !this.editModel?.errorhandler?.type) {
          this.editModel = {
            ...this.editModel,
            errorhandler: cloneDeep(val),
          };
        } else if (!val?.type && this.editModel?.errorhandler) {
          const updated = { ...this.editModel };
          delete updated.errorhandler;
          this.editModel = updated;
        }
      },
      flush: "sync",
    },
    "modelValue.filters": {
      handler(val) {
        this.editModel = {
          ...this.editModel,
          filters: val ? cloneDeep(val) : [],
        };
      },
      deep: true,
    },
  }
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
