<template>
  <div id="fieldcustomeditor" class="col-sm-12">
    <input ref="hiddenFieldInput" type="hidden" :name="name" />

    <div v-if="customFields != null">
      <div
        v-for="(field, index) in customFields"
        :key="index"
        :class="['form-group']"
        data-testid="field-item"
      >
        <label class="col-sm-2 control-label input-sm">{{
          field.label || field.key
        }}</label>
        <div class="col-sm-9">
          <input
            :value="field.value"
            type="text"
            :class="['form-control', 'input-sm', 'context_var_autocomplete']"
            size="100"
            :data-testid="'field-input-' + index"
            @input="onValueInput(field, $event)"
          />
        </div>
        <div class="col-sm-1">
          <span
            class="btn btn-xs btn-default"
            :title="$t('message_delete')"
            @click="removeField(field)"
            data-testid="remove-field-button"
          >
            <i class="glyphicon glyphicon-remove"></i
          ></span>
        </div>
        <div v-if="field.desc" class="col-sm-10 col-sm-offset-2 help-block">
          <div class="help-block" data-testid="field-description-help">
            {{ field.desc }}
          </div>
        </div>
      </div>
    </div>

    <btn
      type="primary"
      @click="openNewField()"
      data-testid="add-field-button"
      >{{ $t("message_addField") }}</btn
    >

    <modal
      id="modal-demo"
      ref="modal"
      v-model="modalAddField"
      title="Add Field"
      ok-text="Save"
      :backdrop="true"
      :dismiss-btn="true"
      :keyboard="true"
      cancel-text="Close"
      append-to-body
      data-testid="modal-title"
    >
      <div class="row" style="padding-left: 30px !important">
        <alert
          v-if="duplicate"
          type="warning"
          data-testid="duplicate-warning"
          ref="duplicateWarningRef"
          ><b>Warning!</b> {{ $t("message_duplicated") }}.</alert
        >
        <alert
          v-if="invalidKey"
          type="warning"
          data-testid="invalid-key-warning"
          ref="invalidKeyWarningRef"
          ><b>Warning!</b> {{ $t("message_fieldKeyRequired") }}.</alert
        >

        <div class="col-md-10">
          <div v-if="useOptions" class="form">
            <div :class="['form-data']">
              <label class="col-md-4">{{ $t("message_select") }}</label>
              <div class="col-md-8">
                <!--
                  append-to="self" keeps the dropdown overlay inside this
                  modal's stacking context. The default ("body") renders it at
                  z-index 1001, below the surrounding modal (1070), which makes
                  the options render but stay unclickable.
                -->
                <pt-select
                  v-model="selectedField"
                  :options="customOptions"
                  option-label="label"
                  :filter="true"
                  :placeholder="$t('message_select')"
                  append-to="self"
                  data-testid="multiselect"
                />
              </div>
            </div>

            <div :class="['form-data']">
              <label
                class="col-md-4"
                :for="`${fieldIdPrefix}-description-input`"
              >
                {{ $t("message_description") }}
              </label>
              <div class="col-md-8">
                <input
                  :id="`${fieldIdPrefix}-description-input`"
                  v-model="newFieldDescription"
                  type="text"
                  :class="['form-control']"
                  :aria-describedby="`${fieldIdPrefix}-description-help`"
                  data-testid="field-description-input"
                />
                <div
                  :id="`${fieldIdPrefix}-description-help`"
                  class="help-block"
                  data-testid="new-field-description-help"
                >
                  {{ $t("message_fieldDescriptionHelp") }}
                </div>
              </div>
            </div>
          </div>

          <div v-if="!useOptions" class="form">
            <div :class="['form-group']">
              <label class="col-md-4" :for="`${fieldIdPrefix}-key-input`">{{
                $t("message_fieldKey")
              }}</label>
              <div class="col-md-8">
                <input
                  :id="`${fieldIdPrefix}-key-input`"
                  v-model="newField"
                  type="text"
                  :class="['form-control']"
                  required
                  aria-required="true"
                  :aria-describedby="`${fieldIdPrefix}-key-help`"
                  data-testid="field-key-input"
                />
                <div
                  :id="`${fieldIdPrefix}-key-help`"
                  class="help-block"
                  data-testid="field-key-help"
                >
                  {{ $t("message_fieldKeyHelp") }}
                </div>
              </div>
            </div>
            <div :class="['form-group']">
              <label class="col-md-4" :for="`${fieldIdPrefix}-label-input`">{{
                $t("message_fieldLabel")
              }}</label>
              <div class="col-md-8">
                <input
                  :id="`${fieldIdPrefix}-label-input`"
                  v-model="newLabelField"
                  type="text"
                  :class="['form-control']"
                  :aria-describedby="`${fieldIdPrefix}-label-help`"
                  data-testid="field-label-input"
                />
                <div
                  :id="`${fieldIdPrefix}-label-help`"
                  class="help-block"
                  data-testid="field-label-help"
                >
                  {{ $t("message_fieldLabelHelp") }}
                </div>
              </div>
            </div>

            <div :class="['form-group']">
              <label
                class="col-md-4"
                :for="`${fieldIdPrefix}-description-input`"
              >
                {{ $t("message_description") }}
              </label>
              <div class="col-md-8">
                <input
                  :id="`${fieldIdPrefix}-description-input`"
                  v-model="newFieldDescription"
                  type="text"
                  :class="['form-control']"
                  :aria-describedby="`${fieldIdPrefix}-description-help`"
                  data-testid="field-description-input"
                />
                <div
                  :id="`${fieldIdPrefix}-description-help`"
                  class="help-block"
                  data-testid="new-field-description-help"
                >
                  {{ $t("message_fieldDescriptionHelp") }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div>
          <button
            type="button"
            class="btn btn-default reset_page_confirm"
            @click="modalAddField = false"
            data-testid="cancel-button"
          >
            {{ $t("message_cancel") }}
          </button>

          <button
            type="button"
            class="btn btn-cta reset_page_confirm"
            @click="addField()"
            data-testid="confirm-add-field-button"
          >
            {{ $t("message_add") }}
          </button>
        </div>
      </template>
    </modal>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { Btn, Alert, Modal } from "uiv";
import PtSelect from "../primeVue/PtSelect/PtSelect.vue";

interface CustomField {
  key?: string;
  label?: string;
  value?: string;
  desc?: string;
}

export default defineComponent({
  name: "DynamicFormPluginProp",
  components: {
    PtSelect,
    Btn,
    Alert,
    Modal,
  },
  props: {
    fields: {
      type: String,
      required: true,
    },
    options: {
      type: String,
      required: false,
    },
    hasOptions: {
      type: String,
      required: true,
    },
    name: {
      type: String,
      required: true,
    },
    // An instance-unique prefix for the modal's control/help ids, e.g.
    // pluginPropEdit.vue's `${rkey}prop_${pindex}_`. `name` alone isn't
    // guaranteed unique per rendered widget - two plugin configurations
    // can share a property name, or names like "foo.bar" and "foo-bar"
    // collide once sanitized - so callers that can render more than one
    // instance on a page should pass this explicitly.
    idPrefix: {
      type: String,
      required: false,
      default: "",
    },
  },
  emits: ["update:modelValue"],
  data() {
    return {
      customFields: [] as CustomField[],
      customOptions: [] as any[],
      useOptions: false,
      modalAddField: false,
      duplicate: false,
      invalidKey: false,
      newField: "",
      newLabelField: "",
      newFieldDescription: "",
      selectedField: { value: "", label: "" },
    };
  },
  computed: {
    // A page can render more than one DynamicFormPluginProp (one per
    // plugin property), so the modal's control/help ids must be unique
    // per instance rather than hard-coded - otherwise every instance's
    // label/aria-describedby resolves to whichever instance rendered
    // first. Prefer the caller-supplied idPrefix (already unique per
    // rendered widget); fall back to a sanitized name for standalone
    // usage (e.g. the dynamic-form demo page) where only one instance
    // is ever on the page at once.
    fieldIdPrefix(): string {
      const prefix = this.idPrefix || this.name.replace(/[^a-zA-Z0-9_-]/g, "-");
      return `dynamic-form-${prefix}`;
    },
  },
  watch: {
    fields(newFields: string) {
      // Keep local state in sync if the prop changes after mount (e.g. the
      // parent round-trips the value through its own v-model chain). Skip
      // when the incoming value already matches what we just emitted
      // ourselves, to avoid fighting with in-flight edits.
      if (newFields === JSON.stringify(this.customFields)) {
        return;
      }
      this.syncFieldsFromProp(newFields);
    },
  },
  mounted() {
    if (this.hasOptions === "true") {
      this.useOptions = true;
    }
    this.syncFieldsFromProp(this.fields);

    if (
      this.useOptions &&
      this.options !== null &&
      this.options !== undefined &&
      this.options !== ""
    ) {
      const optionsObject = JSON.parse(this.options!);
      const options = Object.keys(optionsObject).map((key: any) => {
        const data = optionsObject[key];
        return { value: key, label: data };
      });
      this.customOptions = options;
    }
  },
  beforeUnmount() {
    this.customFields = null as any;
  },
  methods: {
    syncFieldsFromProp(fields: string) {
      if (fields == null || fields === "") {
        // Clearing the prop must clear the list too, otherwise the previous
        // fields stay on screen after the parent resets the value.
        this.customFields = [];
        return;
      }
      const customFieldsObject = JSON.parse(fields);
      if (customFieldsObject != null) {
        let normalizedLabel = false;
        const parsedFields = Object.keys(customFieldsObject).map((key: any) => {
          const value = customFieldsObject[key];
          if (value.desc == null) {
            value.desc = this.$t("message_fieldKeyDescription", [value.key]);
          }
          // Fields saved by the previous editor (or synced in from a parent
          // that hasn't picked up the fallback yet) can carry a blank label.
          // pluginPropView.vue and PluginTagLib.groovy render the stored
          // label as-is, so normalize it here too, not just on creation.
          if (!value.label || value.label.trim() === "") {
            value.label = value.key;
            normalizedLabel = true;
          }
          return value;
        });
        this.customFields = parsedFields;
        if (normalizedLabel) {
          this.refreshPlugin();
        }
      }
    },
    openNewField() {
      // Clear any validation warning left over from a previous, cancelled
      // attempt - otherwise reopening the modal immediately shows it again
      // even though this is a fresh attempt.
      this.duplicate = false;
      this.invalidKey = false;
      this.modalAddField = true;
    },
    addField() {
      let field = {} as CustomField;
      this.duplicate = false;
      this.invalidKey = false;

      const key = this.useOptions ? this.selectedField?.value : this.newField;
      if (!key || key.trim() === "") {
        this.invalidKey = true;
        return;
      }

      if (this.useOptions) {
        if (this.selectedField !== null) {
          const newField = this.selectedField;

          let description = this.newFieldDescription;
          if (description == "") {
            description = this.$t("message_fieldKeyOnlyDescription", [
              newField.value,
            ]);
          } else {
            description = this.$t("message_fieldKeyAppendedDescription", [
              description,
              newField.value,
            ]);
          }

          field = {
            key: newField.value,
            label: newField.label,
            desc: description,
          };
        }
      } else {
        let description = this.newFieldDescription;
        if (description == "") {
          description = this.$t("message_fieldKeyOnlyDescription", [
            this.newField,
          ]);
        } else {
          description = this.$t("message_fieldKeyAppendedDescription", [
            description,
            this.newField,
          ]);
        }

        field = {
          key: this.newField,
          // message_fieldLabelHelp promises the key as the fallback label,
          // so normalize it here rather than only at display time -
          // pluginPropView.vue and PluginTagLib.groovy render the stored
          // label directly and have no fallback of their own.
          label:
            this.newLabelField.trim() !== ""
              ? this.newLabelField
              : this.newField,
          value: "",
          desc: description,
        };
      }
      let exists = false;
      this.customFields.forEach((row: any) => {
        if (field.key === row.key) {
          exists = true;
        }
      });

      if (!exists) {
        this.customFields.push(field);
        this.newField = "";
        this.newLabelField = "";
        this.newFieldDescription = "";
        this.modalAddField = false;
        this.refreshPlugin();
      } else {
        this.duplicate = true;
      }
    },
    removeField(row: any) {
      const fields = [] as CustomField[];
      this.customFields.forEach((field: CustomField) => {
        if (field.key !== row.key) {
          fields.push(field);
        }
      });
      this.customFields = fields;
      this.refreshPlugin();
    },
    onValueInput(field: CustomField, event: Event) {
      field.value = (event.target as HTMLInputElement).value;
      this.refreshPlugin();
    },
    refreshPlugin() {
      const fieldsJson = JSON.stringify(this.customFields);
      const hiddenFieldInput = this.$refs.hiddenFieldInput as HTMLInputElement;
      if (hiddenFieldInput) {
        hiddenFieldInput.value = fieldsJson;
      }
      this.$emit("update:modelValue", fieldsJson);
    },
  },
});
</script>
<style scoped>
.form-data {
  padding-bottom: 30px;
  margin-bottom: 20px;
}
</style>
