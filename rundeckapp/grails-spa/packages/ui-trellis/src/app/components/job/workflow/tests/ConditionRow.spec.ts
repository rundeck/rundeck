import { mount, VueWrapper, flushPromises } from "@vue/test-utils";
import ConditionRow from "../ConditionRow.vue";
import { createEmptyCondition } from "../types/conditionalStepTypes";
import type { Condition, OperatorOption, FieldOption } from "../types/conditionalStepTypes";
import PtSelect from "@/library/components/primeVue/PtSelect/PtSelect.vue";
import PtAutoComplete from "@/library/components/primeVue/PtAutoComplete/PtAutoComplete.vue";

jest.mock("@/library/rundeckService.ts", () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    eventBus: { on: jest.fn(), emit: jest.fn() },
    rdBase: "http://localhost:4440",
  })),
}));

const operatorOptions: OperatorOption[] = [
  { label: "Equal", value: "equals" },
  { label: "Not Equal", value: "notEquals" },
  { label: "Contains", value: "contains" },
];

const fieldOptions: FieldOption[] = [
  { label: "OS Name", value: "os-name" },
  { label: "Region", value: "region" },
];

let wrapper: VueWrapper<any>;

afterEach(() => {
  wrapper?.unmount();
});

const createWrapper = (props = {}): VueWrapper<any> => {
  wrapper = mount(ConditionRow, {
    props: {
      condition: createEmptyCondition(),
      operatorOptions,
      fieldOptions,
      showLabels: true,
      showDeleteButton: true,
      serviceName: "WorkflowNodeStep",
      ...props,
    },
  });
  return wrapper;
};

describe("ConditionRow", () => {
  it("renders the field label text when showLabels is true", () => {
    const wrapper = createWrapper({ showLabels: true });

    expect(wrapper.find('[data-testid="condition-field-label"]').text()).toContain("editConditionalStep.field");
  });

  it("hides all labels when showLabels is false", () => {
    const wrapper = createWrapper({ showLabels: false });

    expect(wrapper.find('[data-testid="condition-field-label"]').classes()).toContain("form-label-hidden");
    expect(wrapper.find('[data-testid="condition-operator-label"]').classes()).toContain("form-label-hidden");
    expect(wrapper.find('[data-testid="condition-value-label"]').classes()).toContain("form-label-hidden");
  });

  it("shows delete button when showDeleteButton is true", () => {
    const wrapper = createWrapper({ showDeleteButton: true });

    expect(wrapper.find('[data-testid="condition-delete-btn"]').exists()).toBe(true);
  });

  it("hides delete button when showDeleteButton is false", () => {
    const wrapper = createWrapper({ showDeleteButton: false });

    expect(wrapper.find('[data-testid="condition-delete-btn"]').exists()).toBe(false);
  });

  it("emits update:condition with field and fieldName when field PtSelect emits a value", async () => {
    const condition = createEmptyCondition();
    const wrapper = createWrapper({ condition });
    const fieldSelect = wrapper.findAllComponents(PtSelect)[0];

    await fieldSelect.vm.$emit("update:modelValue", "os-name");
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted("update:condition")).toBeTruthy();
    const payload = wrapper.emitted("update:condition")![0][0] as any;
    expect(payload.condition.field).toBe("os-name");
    expect(payload.fieldName).toBe("field");
  });

  it("emits update:condition with operator and fieldName when operator PtSelect emits a value", async () => {
    const condition = createEmptyCondition();
    const wrapper = createWrapper({ condition });
    const operatorSelect = wrapper.findAllComponents(PtSelect)[1];

    await operatorSelect.vm.$emit("update:modelValue", "notEquals");
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted("update:condition")).toBeTruthy();
    const payload = wrapper.emitted("update:condition")![0][0] as any;
    expect(payload.condition.operator).toBe("notEquals");
    expect(payload.fieldName).toBe("operator");
  });

  it("emits update:condition with value and fieldName when PtAutoComplete emits a value", async () => {
    const condition = createEmptyCondition();
    const wrapper = createWrapper({ condition });
    const autocomplete = wrapper.findComponent(PtAutoComplete);

    await autocomplete.vm.$emit("update:modelValue", "Linux");
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted("update:condition")).toBeTruthy();
    const payload = wrapper.emitted("update:condition")![0][0] as any;
    expect(payload.condition.value).toBe("Linux");
    expect(payload.fieldName).toBe("value");
  });

  it("emits delete with condition id when delete button is clicked", async () => {
    const condition = createEmptyCondition();
    const wrapper = createWrapper({ condition });

    await wrapper.find('[data-testid="condition-delete-btn"]').trigger("click");

    expect(wrapper.emitted("delete")).toBeTruthy();
    expect(wrapper.emitted("delete")![0][0]).toBe(condition.id);
  });

  describe("error state rendering", () => {
    it("shows field error message when fieldError is provided", () => {
      const wrapper = createWrapper({ fieldError: "Field is required" });

      const errorEl = wrapper.find('[data-testid="pt-select-error"]');
      expect(errorEl.exists()).toBe(true);
      expect(errorEl.text()).toBe("Field is required");
    });

    it("does not show field error message when fieldError is absent", () => {
      const wrapper = createWrapper({ fieldError: undefined });

      expect(wrapper.find('[data-testid="pt-select-error"]').exists()).toBe(false);
    });

    it("shows value error message when valueError is provided", () => {
      const wrapper = createWrapper({ valueError: "Value is required" });

      const errorEl = wrapper.find('[data-testid="pt-autocomplete-error"]');
      expect(errorEl.exists()).toBe(true);
      expect(errorEl.text()).toBe("Value is required");
    });

    it("does not show value error message when valueError is absent", () => {
      const wrapper = createWrapper({ valueError: undefined });

      expect(wrapper.find('[data-testid="pt-autocomplete-error"]').exists()).toBe(false);
    });
  });

  describe("__NOTE__ option selection blocking", () => {
    it("does not emit update:condition when __NOTE__ is selected in field dropdown", async () => {
      const wrapper = createWrapper({ depth: 0 });
      const fieldSelect = wrapper.findAllComponents(PtSelect)[0];

      await fieldSelect.vm.$emit("update:modelValue", "__NOTE__");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("update:condition")).toBeFalsy();
    });

    it("emits update:condition when a real field value is selected", async () => {
      const wrapper = createWrapper({ depth: 0 });
      const fieldSelect = wrapper.findAllComponents(PtSelect)[0];

      await fieldSelect.vm.$emit("update:modelValue", "os-name");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("update:condition")).toBeTruthy();
      const payload = wrapper.emitted("update:condition")![0][0] as any;
      expect(payload.condition.field).toBe("os-name");
      expect(payload.fieldName).toBe("field");
    });
  });

  describe("depth prop and note option visibility", () => {
    async function openFieldDropdown(wrapper: VueWrapper<any>) {
      // Click PrimeVue Select's root element (pt-select-control falls through to .p-select div)
      // which triggers PrimeVue's internal click-to-open handler
      const fieldPtSelect = wrapper.findAllComponents(PtSelect)[0];
      await fieldPtSelect.find('[data-testid="pt-select-control"]').trigger("click");
      await flushPromises();
    }

    it("includes the __NOTE__ option in the field dropdown when depth is 0", async () => {
      const wrapper = createWrapper({ depth: 0 });
      await openFieldDropdown(wrapper);

      // Option slots render in the teleported overlay — query document.body
      const noteLink = document.body.querySelector('[data-testid="condition-note-link"]');
      expect(noteLink).not.toBeNull();
    });

    it("does not include the __NOTE__ option in the field dropdown when depth is 1", async () => {
      const wrapper = createWrapper({ depth: 1 });
      await openFieldDropdown(wrapper);

      const noteLink = document.body.querySelector('[data-testid="condition-note-link"]');
      expect(noteLink).toBeNull();
    });

    it("emits switch-step-type when the note link is clicked", async () => {
      const wrapper = createWrapper({ depth: 0 });
      await openFieldDropdown(wrapper);

      const noteLink = document.body.querySelector('[data-testid="condition-note-link"]') as HTMLElement;
      expect(noteLink).not.toBeNull();
      noteLink.click();
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("switch-step-type")).toBeTruthy();
    });
  });
});
