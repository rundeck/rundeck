import { mount, flushPromises, VueWrapper } from "@vue/test-utils";
import DynamicFormPluginProp from "../DynamicFormPluginProp.vue";
import { Btn, Modal, Alert } from "uiv";
import VueMultiselect from "vue-multiselect";

const mockElement = document.createElement("input");
mockElement.id = "test-element";
document.body.appendChild(mockElement);

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
      element: "test-element",
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
      components: { Btn, Modal, Alert, VueMultiselect },
      mocks: {
        $t: (msg: string) => msg,
      },
      stubs: {
        Modal: {
          template: `<div data-testid="modal-title"><slot></slot><slot name="footer"></slot>Add Field</div>`,
        },

        Alert: {
          template: `<div ref="duplicateWarningRef">Duplicate warning text</div>`,
        },
        VueMultiselect: {
          template: `<div data-testid="multiselect">Multiselect Stub</div>`,
        },
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
    const multiselect = wrapper.findComponent(VueMultiselect);
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
    expect(fields.at(1)?.find("label").text()).toBe("Field 1");
    expect(helpBlocks.at(1)?.text()).toBe(
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
    const multiselect = wrapper.findComponent(VueMultiselect);
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
});
