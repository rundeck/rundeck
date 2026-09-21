import { mount, flushPromises, VueWrapper } from "@vue/test-utils";
import DynamicFormPluginProp from "../DynamicFormPluginProp.vue";
import { Btn, Modal, Alert } from "uiv";
import PtSelect from "../../primeVue/PtSelect/PtSelect.vue";

// The global $t mock returns the key and drops the arguments, which would
// make the generated descriptions unverifiable. Interpolate the real en_US
// templates instead so the assertions below still check the composed text.
const messages: Record<string, string> = {
  message_fieldKeyDescription: "Field key: {0}",
  message_fieldKeyOnlyDescription: "Field key {0}",
  message_fieldKeyAppendedDescription: "{0} (Field key: {1})",
};
const translate = (key: string, params: string[] = []) =>
  (messages[key] ?? key).replace(
    /\{(\d+)\}/g,
    (_match, index) => params[Number(index)],
  );

const createWrapper = (props = {}) => {
  return mount(DynamicFormPluginProp, {
    props: {
      fields: JSON.stringify({
        field1: {
          key: "field1",
          label: "Option1",
          value: "field1",
          desc: "Description1",
        },
      }),
      options: JSON.stringify({ field1: ["option1", "option2"] }),
      hasOptions: "true",
      name: "test-name",
      ...props,
    },
    data() {
      return {
        newField: "field1",
      };
    },
    global: {
      mocks: { $t: translate },
      components: { Btn, Modal, Alert, PtSelect },
      stubs: {
        Modal: {
          template: `<div data-testid="modal-title"><slot></slot><slot name="footer"></slot>Add Field</div>`,
        },

        Alert: {
          template: `<div ref="duplicateWarningRef">Duplicate warning text</div>`,
        },
        // PtSelect is deliberately NOT stubbed: the previous vue-multiselect
        // stub is what hid RUN-4764 (a render crash in the real select
        // component) from this suite.
      },
    },
    attachTo: document.body,
  });
};

describe("DynamicFormPluginProp.vue", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });
  it("opens the modal when 'Add Field' button is clicked", async () => {
    const wrapper = createWrapper();
    await wrapper.find('[data-testid="add-field-button"]').trigger("click");
    await flushPromises();
    const modalTitle = wrapper.find('[data-testid="modal-title"]');
    expect(modalTitle.text()).toContain("Add Field");
  });
  it("adds a new field through modal interaction", async () => {
    const wrapper = createWrapper();
    await wrapper.find('[data-testid="add-field-button"]').trigger("click");
    await flushPromises();
    const multiselect = wrapper.findComponent(PtSelect);
    await (multiselect as VueWrapper<any>).vm.$emit("update:modelValue", {
      value: "Option1",
      label: "Field 1",
    });
    await flushPromises();
    const descriptionInput = wrapper.find(
      '[data-testid="field-description-input"]',
    );
    await descriptionInput.setValue("New Field Description");
    await wrapper
      .find('[data-testid="confirm-add-field-button"]')
      .trigger("click");
    await flushPromises();
    const fields = wrapper.findAll('[data-testid="field-item"]');
    const helpBlocks = fields.at(1)?.findAll(".help-block");
    expect(fields.length).toBe(2);
    expect(fields.at(1)?.find("label")?.text()).toBe("Field 1");
    expect(helpBlocks?.at(1)?.text()).toBe(
      "New Field Description (Field key: Option1)",
    );
  });
  it("removes a field when the remove button is clicked", async () => {
    const wrapper = createWrapper();
    await flushPromises();
    const initialFieldLabel = wrapper
      .find('[data-testid="field-item"] label')
      .text();
    expect(initialFieldLabel).toBe("Option1");
    await wrapper.find('[data-testid="remove-field-button"]').trigger("click");
    await flushPromises();
    // Check if the field is removed by verifying there are no form groups left
    expect(wrapper.findAll('[data-testid="field-item"]').length).toBe(0);
  });
  it("displays a warning for duplicate fields", async () => {
    const wrapper = createWrapper();
    await flushPromises();
    // Ensure the modal opens by clicking the button
    const addField = await wrapper.find('[data-testid="add-field-button"]');
    await addField.trigger("click");
    await wrapper.vm.$nextTick();
    const multiselect = wrapper.findComponent(PtSelect);
    await multiselect.vm.$emit("update:modelValue", {
      value: "field1",
      label: "Field 1",
    });
    await wrapper.vm.$nextTick();
    // Confirm adding the field
    const confirmAddField = wrapper.find(
      '[data-testid="confirm-add-field-button"]',
    );
    await confirmAddField.trigger("click");
    await flushPromises();
    const warningMessage = wrapper.find('[data-testid="duplicate-warning"]');
    expect(warningMessage.text()).toBe("Duplicate warning text");
  });
  it("updates field value on input change", async () => {
    const wrapper = createWrapper();
    await flushPromises();
    const inputField = wrapper.find('[data-testid="field-input-0"]');
    expect(inputField.exists()).toBe(true);
    await inputField.setValue("Updated Value");
    await flushPromises();
    const updatedFieldValue = (wrapper.vm as any).customFields[0].value;
    expect(updatedFieldValue).toBe("Updated Value");
  });

  it("emits the updated value on every keystroke, without waiting for blur", async () => {
    // Regression test: the field value used to be synced to the parent
    // (and therefore to the saved job/step config) only on the input's
    // 'change' event, which fires on blur. Typing a value and saving
    // without first clicking/tabbing away silently dropped it. The value
    // must now be emitted on 'input' directly.
    const wrapper = createWrapper();
    await flushPromises();
    const inputField = wrapper.find('[data-testid="field-input-0"]');
    await inputField.setValue("Updated Value");
    await flushPromises();
    const emitted = wrapper.emitted("update:modelValue");
    expect(emitted).toBeTruthy();
    const lastEmittedFields = JSON.parse(
      emitted![emitted!.length - 1][0] as string,
    );
    expect(lastEmittedFields[0].value).toBe("Updated Value");
  });

  it("shows help text for the Field Label and Field Key inputs on the free-text path", async () => {
    // The "Add Field" modal previously gave no guidance on what these two
    // inputs mean or whether they're required, which made it easy to add a
    // field with a blank key (silently dropped/meaningless downstream) or
    // to not realize the label is optional and falls back to the key.
    const wrapper = createWrapper({ hasOptions: "false" });
    await wrapper.find('[data-testid="add-field-button"]').trigger("click");
    await flushPromises();

    expect(wrapper.find('[data-testid="field-label-help"]').text()).toContain(
      "message_fieldLabelHelp",
    );
    expect(wrapper.find('[data-testid="field-key-help"]').text()).toContain(
      "message_fieldKeyHelp",
    );
  });

  it("associates the Field Key and Field Label inputs with their labels and help text, and marks Key required", async () => {
    // Copilot review on RUN-4980: the labels had no `for`, the inputs no
    // `id`/`aria-describedby`, and the required Key exposed no required
    // state - so screen readers announced neither the field name nor the
    // guidance on focus.
    const wrapper = createWrapper({ hasOptions: "false" });
    await wrapper.find('[data-testid="add-field-button"]').trigger("click");
    await flushPromises();

    const keyInput = wrapper.find('[data-testid="field-key-input"]');
    const keyHelp = wrapper.find('[data-testid="field-key-help"]');
    expect(keyInput.attributes("id")).toBeTruthy();
    expect(keyInput.attributes("aria-describedby")).toBe(
      keyHelp.attributes("id"),
    );
    expect(keyInput.attributes("required")).toBeDefined();
    expect(keyInput.attributes("aria-required")).toBe("true");

    const labelInput = wrapper.find('[data-testid="field-label-input"]');
    const labelHelp = wrapper.find('[data-testid="field-label-help"]');
    expect(labelInput.attributes("id")).toBeTruthy();
    expect(labelInput.attributes("aria-describedby")).toBe(
      labelHelp.attributes("id"),
    );

    const descriptionInput = wrapper.find(
      '[data-testid="field-description-input"]',
    );
    const descriptionHelp = wrapper.find(
      '[data-testid="new-field-description-help"]',
    );
    expect(descriptionInput.attributes("id")).toBeTruthy();
    expect(descriptionInput.attributes("aria-describedby")).toBe(
      descriptionHelp.attributes("id"),
    );

    const keyLabelEl = wrapper
      .findAll("label")
      .find((l) => l.attributes("for") === keyInput.attributes("id"));
    const fieldLabelEl = wrapper
      .findAll("label")
      .find((l) => l.attributes("for") === labelInput.attributes("id"));
    const descriptionLabelEl = wrapper
      .findAll("label")
      .find((l) => l.attributes("for") === descriptionInput.attributes("id"));
    expect(keyLabelEl).toBeTruthy();
    expect(fieldLabelEl).toBeTruthy();
    expect(descriptionLabelEl).toBeTruthy();
  });

  it("scopes the modal's control/help ids to the plugin property's own name, so multiple instances on a page don't collide", async () => {
    // Copilot review on RUN-4980: the ids were hard-coded, so a page with
    // more than one DynamicFormPluginProp instance (one per plugin
    // property) would have every instance's label/aria-describedby
    // resolve to whichever instance rendered first.
    const wrapperA = createWrapper({ hasOptions: "false", name: "fieldA" });
    const wrapperB = createWrapper({ hasOptions: "false", name: "fieldB" });
    await wrapperA.find('[data-testid="add-field-button"]').trigger("click");
    await wrapperB.find('[data-testid="add-field-button"]').trigger("click");
    await flushPromises();

    const idA = wrapperA
      .find('[data-testid="field-key-input"]')
      .attributes("id");
    const idB = wrapperB
      .find('[data-testid="field-key-input"]')
      .attributes("id");

    expect(idA).toBeTruthy();
    expect(idB).toBeTruthy();
    expect(idA).not.toBe(idB);
  });

  it("blocks adding a field with a blank Key on the free-text path and shows a validation warning", async () => {
    // Copilot review on RUN-4980: the help text says the Field Key is
    // required, but confirming with a blank key previously still added an
    // unusable, empty-key entry. It must now be rejected instead.
    const wrapper = createWrapper({ hasOptions: "false" });
    await wrapper.find('[data-testid="add-field-button"]').trigger("click");
    await flushPromises();

    // createWrapper's data() override pre-seeds newField as "field1" for
    // other tests' benefit - clear it explicitly so this test's key is
    // actually blank.
    await wrapper.find('[data-testid="field-key-input"]').setValue("");
    await wrapper
      .find('[data-testid="field-label-input"]')
      .setValue("Some Label");
    await wrapper
      .find('[data-testid="confirm-add-field-button"]')
      .trigger("click");
    await flushPromises();

    expect(wrapper.findAll('[data-testid="field-item"]').length).toBe(1);
    expect(wrapper.find('[data-testid="invalid-key-warning"]').exists()).toBe(
      true,
    );
  });

  it("blocks adding a field via the options path when nothing is selected", async () => {
    const wrapper = createWrapper();
    await wrapper.find('[data-testid="add-field-button"]').trigger("click");
    await flushPromises();

    await wrapper
      .find('[data-testid="confirm-add-field-button"]')
      .trigger("click");
    await flushPromises();

    expect(wrapper.findAll('[data-testid="field-item"]').length).toBe(1);
    expect(wrapper.find('[data-testid="invalid-key-warning"]').exists()).toBe(
      true,
    );
  });

  it("clears a stale invalid-key warning when the modal is reopened", async () => {
    // Copilot review on RUN-4980: openNewField() didn't reset invalidKey,
    // so after a blank-key submission was cancelled, reopening the modal
    // for a new attempt immediately showed the previous warning again.
    const wrapper = createWrapper({ hasOptions: "false" });
    await wrapper.find('[data-testid="add-field-button"]').trigger("click");
    await flushPromises();

    await wrapper.find('[data-testid="field-key-input"]').setValue("");
    await wrapper
      .find('[data-testid="confirm-add-field-button"]')
      .trigger("click");
    await flushPromises();
    expect(wrapper.find('[data-testid="invalid-key-warning"]').exists()).toBe(
      true,
    );

    await wrapper.find('[data-testid="add-field-button"]').trigger("click");
    await flushPromises();

    expect(wrapper.find('[data-testid="invalid-key-warning"]').exists()).toBe(
      false,
    );
  });

  it("shows help text for the Description input using its own key, not the reused message_empty", async () => {
    // Copilot review on RUN-4980: this help text used to reuse message_empty,
    // but other locale catalogues already translate that key as just "Can be
    // empty" and take priority over the en_US fallback, hiding the new
    // guidance from non-English users. It now has a dedicated key instead.
    const wrapper = createWrapper({ hasOptions: "false" });
    await wrapper.find('[data-testid="add-field-button"]').trigger("click");
    await flushPromises();

    expect(
      wrapper.find('[data-testid="new-field-description-help"]').text(),
    ).toBe("message_fieldDescriptionHelp");
  });

  it("falls back the stored label to the Key when the Field Label is left blank on the free-text path", async () => {
    // Copilot review on RUN-4980: message_fieldLabelHelp promises the Field
    // Key as the label fallback, but the field used to be serialized with a
    // literal blank label. pluginPropView.vue and PluginTagLib.groovy render
    // the stored label as-is (no fallback of their own), so the emitted
    // JSON must carry the fallback, not just this editor's own display.
    const wrapper = createWrapper({ hasOptions: "false" });
    await wrapper.find('[data-testid="add-field-button"]').trigger("click");
    await flushPromises();

    await wrapper.find('[data-testid="field-key-input"]').setValue("env_name");
    await wrapper
      .find('[data-testid="confirm-add-field-button"]')
      .trigger("click");
    await flushPromises();

    const emitted = wrapper.emitted("update:modelValue");
    const lastEmittedFields = JSON.parse(
      emitted![emitted!.length - 1][0] as string,
    );
    expect(lastEmittedFields[1].label).toBe("env_name");
  });

  it("normalizes a blank label on an existing (legacy) field to its Key on load, and re-emits it", async () => {
    // Copilot review on RUN-4980: the fallback above only covered newly
    // created free-text fields. Fields already saved with `label: ""` by
    // the previous editor were left untouched by syncFieldsFromProp(), so
    // downstream renderers (pluginPropView.vue, PluginTagLib.groovy) would
    // still show them with no label.
    const wrapper = createWrapper({
      fields: JSON.stringify({
        legacy_field: {
          key: "legacy_field",
          label: "",
          value: "",
          desc: "Legacy description",
        },
      }),
    });
    await flushPromises();

    expect(wrapper.find('[data-testid="field-item"] label').text()).toBe(
      "legacy_field",
    );

    const emitted = wrapper.emitted("update:modelValue");
    expect(emitted).toBeTruthy();
    const lastEmittedFields = JSON.parse(
      emitted![emitted!.length - 1][0] as string,
    );
    expect(lastEmittedFields[0].label).toBe("legacy_field");
  });

  describe("regression for RUN-4764", () => {
    it("adds a field via the free-text Field Label/Field Key path without throwing", async () => {
      // hasOptions "false" is the free-text path, used whenever the plugin
      // supplies no allowed values. The tests above only covered the
      // select path, so this branch was previously untested.
      const wrapper = createWrapper({
        hasOptions: "false",
      });
      await wrapper.find('[data-testid="add-field-button"]').trigger("click");
      await flushPromises();

      await wrapper
        .find('[data-testid="field-label-input"]')
        .setValue("Root Cause");
      await wrapper
        .find('[data-testid="field-key-input"]')
        .setValue("u_root_cause");
      await wrapper
        .find('[data-testid="confirm-add-field-button"]')
        .trigger("click");
      await flushPromises();

      const fields = wrapper.findAll('[data-testid="field-item"]');
      expect(fields.length).toBe(2);
      expect(fields.at(1)?.find("label")?.text()).toBe("Root Cause");
    });

    it("clears the list when the fields prop is emptied after mount", async () => {
      const wrapper = createWrapper();
      await flushPromises();
      expect(wrapper.findAll('[data-testid="field-item"]').length).toBe(1);

      await wrapper.setProps({ fields: "" });
      await flushPromises();

      expect(wrapper.findAll('[data-testid="field-item"]').length).toBe(0);
    });

    it("syncs the fields prop reactively when it changes after mount", async () => {
      const wrapper = createWrapper();
      await flushPromises();
      expect(wrapper.findAll('[data-testid="field-item"]').length).toBe(1);

      await wrapper.setProps({
        fields: JSON.stringify({
          field1: {
            key: "field1",
            label: "Option1",
            value: "field1",
            desc: "Description1",
          },
          field2: {
            key: "field2",
            label: "Option2",
            value: "field2",
            desc: "Description2",
          },
        }),
      });
      await flushPromises();

      const fields = wrapper.findAll('[data-testid="field-item"]');
      expect(fields.length).toBe(2);
      expect(fields.at(1)?.find("label")?.text()).toBe("Option2");
    });
  });
});
