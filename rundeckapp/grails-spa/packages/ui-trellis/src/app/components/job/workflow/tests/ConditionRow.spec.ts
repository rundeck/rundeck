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
  { label: "Equal", value: "equal" },
  { label: "Not Equal", value: "notEqual" },
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
    expect(wrapper.findAll("label").length).toBe(0);
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
    const emittedCondition = wrapper.emitted("update:condition")?.[0]?.[0] as Condition;
    expect(emittedCondition.field).toBe("os-name");
  });

  it("emits update:condition when operator is changed", async () => {
    const condition = createEmptyCondition();
    const wrapper = createWrapper({ condition });
    
    const select = wrapper.find(".operator-column select");
    await select.setValue("notEqual");
    
    expect(wrapper.emitted("update:condition")).toBeTruthy();
    const emittedCondition = wrapper.emitted("update:condition")?.[0]?.[0] as Condition;
    expect(emittedCondition.operator).toBe("notEqual");
  });

  it("emits update:condition when value is changed", async () => {
    const condition = createEmptyCondition();
    const wrapper = createWrapper({ condition });
    
    const input = wrapper.find(".value-column input");
    await input.setValue("Linux");
    
    expect(wrapper.emitted("update:condition")).toBeTruthy();
    const emittedCondition = wrapper.emitted("update:condition")?.[0]?.[0] as Condition;
    expect(emittedCondition.value).toBe("Linux");
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
      operator: "equal",
      value: "Linux",
    };
    const wrapper = createWrapper({ condition });
    
    const valueInput = wrapper.find(".value-column input");
    expect((valueInput.element as HTMLInputElement).value).toBe("Linux");
  });
});
