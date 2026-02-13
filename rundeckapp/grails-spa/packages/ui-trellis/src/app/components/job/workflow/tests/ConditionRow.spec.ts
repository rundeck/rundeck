import { mount, VueWrapper } from "@vue/test-utils";
import ConditionRow from "../ConditionRow.vue";
import { createEmptyCondition } from "../types/conditionalStepTypes";
import type { Condition, OperatorOption, FieldOption } from "../types/conditionalStepTypes";

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

const createWrapper = (props = {}): VueWrapper<any> => {
  return mount(ConditionRow, {
    props: {
      condition: createEmptyCondition(),
      operatorOptions,
      fieldOptions,
      showLabels: true,
      showDeleteButton: true,
      serviceName: "WorkflowNodeStep",
      ...props,
    },
    global: {
      stubs: {
        PtSelect: {
          template: `<select :value="modelValue" @change="$emit('update:modelValue', $event.target.value)">
            <option v-for="opt in options" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>`,
          props: ["modelValue", "options", "optionLabel", "optionValue", "placeholder"],
        },
        PtAutoComplete: {
          template: `<input :value="modelValue" @input="$emit('update:modelValue', $event.target.value)" />`,
          props: ["modelValue", "placeholder", "suggestions", "tabMode", "tabs", "replaceOnSelect"],
        },
        PtButton: {
          template: `<button @click="$emit('click')"><slot /></button>`,
          props: ["outlined", "severity", "icon"],
        },
        InputText: {
          template: `<input :value="modelValue" @input="$emit('update:modelValue', $event.target.value)" />`,
          props: ["modelValue", "placeholder"],
        },
      },
      mocks: {
        $t: (key: string) => key,
      },
    },
  });
};

describe("ConditionRow", () => {
  it("renders with default props", () => {
    const wrapper = createWrapper();
    expect(wrapper.exists()).toBe(true);
    expect(wrapper.find(".condition-row").exists()).toBe(true);
  });

  it("shows labels when showLabels is true", () => {
    const wrapper = createWrapper({ showLabels: true });
    expect(wrapper.findAll("label").length).toBeGreaterThan(0);
  });

  it("hides labels when showLabels is false", () => {
    const wrapper = createWrapper({ showLabels: false });
    // Labels are still rendered but hidden with CSS (visibility: hidden)
    const labels = wrapper.findAll("label");
    expect(labels.length).toBeGreaterThan(0);
    // Check that the labels have the form-label-hidden class
    labels.forEach(label => {
      expect(label.classes()).toContain("form-label-hidden");
    });
  });

  it("shows delete button when showDeleteButton is true", () => {
    const wrapper = createWrapper({ showDeleteButton: true });
    expect(wrapper.find(".delete-button").exists()).toBe(true);
  });

  it("hides delete button when showDeleteButton is false", () => {
    const wrapper = createWrapper({ showDeleteButton: false });
    expect(wrapper.find(".delete-button").exists()).toBe(false);
  });

  it("emits update:condition when field is changed", async () => {
    const condition = createEmptyCondition();
    const wrapper = createWrapper({ condition });
    
    const select = wrapper.find(".field-column select");
    await select.setValue("os-name");
    
    expect(wrapper.emitted("update:condition")).toBeTruthy();
    const emittedPayload = wrapper.emitted("update:condition")?.[0]?.[0] as any;
    expect(emittedPayload.condition.field).toBe("os-name");
    expect(emittedPayload.fieldName).toBe("field");
  });

  it("emits update:condition when operator is changed", async () => {
    const condition = createEmptyCondition();
    const wrapper = createWrapper({ condition });
    
    const select = wrapper.find(".operator-column select");
    await select.setValue("notEquals");
    
    expect(wrapper.emitted("update:condition")).toBeTruthy();
    const emittedPayload = wrapper.emitted("update:condition")?.[0]?.[0] as any;
    expect(emittedPayload.condition.operator).toBe("notEquals");
    expect(emittedPayload.fieldName).toBe("operator");
  });

  it("emits update:condition when value is changed", async () => {
    const condition = createEmptyCondition();
    const wrapper = createWrapper({ condition });

    const input = wrapper.find(".value-column input");
    await input.setValue("Linux");

    expect(wrapper.emitted("update:condition")).toBeTruthy();
    const emittedPayload = wrapper.emitted("update:condition")?.[0]?.[0] as any;
    expect(emittedPayload.condition.value).toBe("Linux");
    expect(emittedPayload.fieldName).toBe("value");
  });

  it("emits delete when delete button is clicked", async () => {
    const condition = createEmptyCondition();
    const wrapper = createWrapper({ condition });
    
    await wrapper.find(".delete-button").trigger("click");
    
    expect(wrapper.emitted("delete")).toBeTruthy();
    expect(wrapper.emitted("delete")?.[0]?.[0]).toBe(condition.id);
  });

  it("displays the correct condition values", () => {
    const condition: Condition = {
      id: "test-id",
      field: "os-name",
      operator: "equals",
      value: "Linux",
    };
    const wrapper = createWrapper({ condition });

    const valueInput = wrapper.find(".value-column input");
    expect((valueInput.element as HTMLInputElement).value).toBe("Linux");
  });

  describe("depth prop and switch step type visibility", () => {
    it("shows switch step type note when depth is 0 (root conditional)", () => {
      const wrapper = createWrapper({ depth: 0 });
      const fieldOptions = (wrapper.vm as any).fieldOptionsWithNote;

      // Should have the note option prepended
      expect(fieldOptions[0].isNote).toBe(true);
      expect(fieldOptions[0].value).toBe("__NOTE__");
    });

    it("hides switch step type note when depth is 1 (nested conditional)", () => {
      const wrapper = createWrapper({ depth: 1 });
      const fieldOptions = (wrapper.vm as any).fieldOptionsWithNote;

      // Should NOT have the note option
      const hasNoteOption = fieldOptions.some((opt: FieldOption) => opt.isNote);
      expect(hasNoteOption).toBe(false);
    });

    it("hides switch step type note when depth is 2 or higher", () => {
      const wrapper = createWrapper({ depth: 2 });
      const fieldOptions = (wrapper.vm as any).fieldOptionsWithNote;

      // Should NOT have the note option
      const hasNoteOption = fieldOptions.some((opt: FieldOption) => opt.isNote);
      expect(hasNoteOption).toBe(false);
    });

    it("emits switch-step-type when note link is clicked at depth 0", async () => {
      const wrapper = createWrapper({ depth: 0 });

      // Verify switch-step-type event can be emitted
      await wrapper.vm.handleNoteClick();
      expect(wrapper.emitted("switch-step-type")).toBeTruthy();
    });
  });
});
