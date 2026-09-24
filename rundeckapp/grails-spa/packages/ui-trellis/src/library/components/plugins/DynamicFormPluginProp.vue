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
          field.label?.trim() || field.key
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
            data-testid="remove-field-button"
            @click="removeField(field)"
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
      data-testid="add-field-button"
      @click="openNewField()"
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
          ref="duplicateWarningRef"
          type="warning"
          data-testid="duplicate-warning"
          ><b>{{ $t("message_warning") }}</b>
          {{ $t("message_duplicated") }}.</alert
        >
        <alert
          v-if="invalidKey"
          ref="invalidKeyWarningRef"
          type="warning"
          data-testid="invalid-key-warning"
          ><b>{{ $t("message_warning") }}</b>
          {{ $t("message_fieldKeyRequired") }}.</alert
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
              <label class="col-md-4" :for="`${uid}-description-input`">
                {{ $t("message_description") }}
              </label>
              <div class="col-md-8">
                <input
                  :id="`${uid}-description-input`"
                  v-model="newFieldDescription"
                  type="text"
                  :class="['form-control']"
                  :aria-describedby="`${uid}-description-help`"
                  data-testid="field-description-input"
                />
                <div
                  :id="`${uid}-description-help`"
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
              <label class="col-md-4" :for="`${uid}-key-input`">{{
                $t("message_fieldKey")
              }}</label>
              <div class="col-md-8">
                <input
                  :id="`${uid}-key-input`"
                  v-model="newField"
                  type="text"
                  :class="['form-control']"
                  required
                  :aria-describedby="`${uid}-key-help`"
                  data-testid="field-key-input"
                />
                <div
                  :id="`${uid}-key-help`"
                  class="help-block"
                  data-testid="field-key-help"
                >
                  {{ $t("message_fieldKeyHelp") }}
                </div>
              </div>
            </div>
            <div :class="['form-group']">
              <label class="col-md-4" :for="`${uid}-label-input`">{{
                $t("message_fieldLabel")
              }}</label>
              <div class="col-md-8">
                <input
                  :id="`${uid}-label-input`"
                  v-model="newLabelField"
                  type="text"
                  :class="['form-control']"
                  :aria-describedby="`${uid}-label-help`"
                  data-testid="field-label-input"
                />
                <div
                  :id="`${uid}-label-help`"
                  class="help-block"
                  data-testid="field-label-help"
                >
                  {{ $t("message_fieldLabelHelp") }}
                </div>
              </div>
            </div>

            <div :class="['form-group']">
              <label class="col-md-4" :for="`${uid}-description-input`">
                {{ $t("message_description") }}
              </label>
              <div class="col-md-8">
                <input
                  :id="`${uid}-description-input`"
                  v-model="newFieldDescription"
                  type="text"
                  :class="['form-control']"
                  :aria-describedby="`${uid}-description-help`"
                  data-testid="field-description-input"
                />
                <div
                  :id="`${uid}-description-help`"
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
            data-testid="cancel-button"
            @click="modalAddField = false"
          >
            {{ $t("message_cancel") }}
          </button>

          <button
            type="button"
            class="btn btn-cta reset_page_confirm"
            data-testid="confirm-add-field-button"
            @click="addField()"
          >
            {{ $t("message_add") }}
          </button>
        </div>
      </template>
    </modal>
  </div>
</template>

<script lang="ts">
import { defineComponent, useId } from "vue";
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
  },
  emits: ["update:modelValue"],
  setup() {
    return { uid: useId() };
  },
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
  watch: {
    fields(newFields: string) {
      // Skip syncing a value we just emitted ourselves, to avoid fighting in-flight edits.
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
        this.customFields = [];
        return;
      }
      const customFieldsObject = JSON.parse(fields);
      if (customFieldsObject != null) {
        this.customFields = Object.keys(customFieldsObject).map((key: any) => {
          const value = customFieldsObject[key];
          if (value.desc == null) {
            value.desc = this.$t("message_fieldKeyDescription", [value.key]);
          }
          return value;
        });
      }
    },
    openNewField() {
      this.duplicate = false;
      this.invalidKey = false;
      this.modalAddField = true;
    },
    addField() {
      let field = {} as CustomField;
      this.duplicate = false;
      this.invalidKey = false;

      const key = (
        this.useOptions ? this.selectedField?.value : this.newField
      )?.trim();
      if (!key) {
        this.invalidKey = true;
        return;
      }

      if (this.useOptions) {
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
      } else {
        let description = this.newFieldDescription;
        if (description == "") {
          description = this.$t("message_fieldKeyOnlyDescription", [key]);
        } else {
          description = this.$t("message_fieldKeyAppendedDescription", [
            description,
            key,
          ]);
        }

        field = {
          key,
          // Falls back to the key, per message_fieldLabelHelp.
          label: this.newLabelField.trim() !== "" ? this.newLabelField : key,
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
