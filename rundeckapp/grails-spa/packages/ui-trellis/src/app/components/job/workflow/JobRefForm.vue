<template>
  <modal
    v-model="showModal"
    data-testid="jobref-modal"
    size="lg"
    :title="$t('plugin.edit.title')"
    :keyboard="false"
  >
    <div v-if="error" class="alert alert-danger">
      <ErrorsList :errors="[errorMessage]" />
    </div>

    <JobRefFormFields
      v-model="editModel.jobref"
      :show-validation="showRequired"
      :extra-autocomplete-vars="extraAutocompleteVars"
    />

    <slot name="extra" />
    <template #footer>
      <btn data-testid="cancel-button" @click="$emit('cancel')">
        {{ $t("Cancel") }}
      </btn>
      <btn type="success" data-testid="save-button" @click="saveChanges">
        {{ $t("Save") }}
      </btn>
    </template>
  </modal>
</template>

<script lang="ts">
import { defineComponent, type PropType } from "vue";
import { PluginConfig } from "@/library/interfaces/PluginConfig";
import { getRundeckContext } from "@/library";
import { JobRefData } from "@/app/components/job/workflow/types/workflowTypes";
import { merge } from "lodash";
import ErrorsList from "@/app/components/job/options/ErrorsList.vue";
import { ContextVariable } from "@/library/stores/contextVariables";
import JobRefFormFields from "./JobRefFormFields.vue";

const rundeckContext = getRundeckContext();

export default defineComponent({
  name: "JobRefForm",
  components: {
    ErrorsList,
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
    modalActive: {
      type: Boolean,
      default: true,
    },
    extraAutocompleteVars: {
      type: Array as PropType<ContextVariable[]>,
      required: false,
      default: () => [],
    },
  },
  emits: ["update:modelValue", "update:modalActive", "save", "cancel"],
  data() {
    return {
      showModal: this.modalActive,
      editModel: {
        description: "",
        keepgoingOnSuccess: false,
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
      } as JobRefData,
      error: false,
      errorMessage: "",
      showRequired: false,
    };
  },
  mounted() {
    // Initialize editModel with incoming modelValue on mount
    this.editModel = merge(this.editModel, this.modelValue);
  },
  watch: {
    modalActive(val) {
      this.showModal = val;
    },
    showModal(val) {
      this.$emit("update:modalActive", val);
    },
    modelValue(val) {
      this.editModel = merge(this.editModel, val);
    },
  },
  methods: {
    async saveChanges() {
      if (
        this.editModel.jobref.name.length === 0 &&
        this.editModel.jobref.uuid.length === 0
      ) {
        this.error = true;
        this.errorMessage = this.$t("commandExec.jobName.blank.message");
        this.showRequired = true;
      } else {
        this.error = false;
        this.showRequired = false;
      }

      if (this.error) {
        return;
      }

      this.$emit("update:modelValue", this.editModel);
      this.$emit("save");
    },
  },
});
</script>
