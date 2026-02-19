import { mount, flushPromises } from "@vue/test-utils";
import ConditionsEditor from "../ConditionsEditor.vue";
import ConditionRow from "../ConditionRow.vue";
import PtAutoComplete from "@/library/components/primeVue/PtAutoComplete/PtAutoComplete.vue";
import {
  mockSingleConditionSet,
  mockTwoConditionSets,
  mockSetWithTwoConditions,
  mockSetAtMaxConditions,
  mockFiveConditionSets,
} from "./mocks/conditionsEditorMocks";

jest.mock("@/library/stores/contextVariables", () => ({
  contextVariables: jest.fn().mockReturnValue({
    job: [{ name: "id", title: "Job ID", type: "job" }],
    node: [{ name: "hostname", title: "Hostname", type: "node" }],
  }),
}));

jest.mock("@/app/utilities/loadJsonData", () => ({
  loadJsonData: jest.fn().mockReturnValue(null),
}));

jest.mock("@/library/stores/Plugins", () => ({
  ServiceType: {
    WorkflowNodeStep: "WorkflowNodeStep",
    WorkflowStep: "WorkflowStep",
  },
}));

jest.mock("@/library/rundeckService.ts", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    eventBus: { on: jest.fn(), emit: jest.fn() },
  }),
}));

let wrapper: ReturnType<typeof mount>;

afterEach(() => {
  wrapper?.unmount();
});

const createWrapper = (options: { props?: Record<string, any> } = {}) => {
  wrapper = mount(ConditionsEditor, {
    props: {
      modelValue: mockSingleConditionSet,
      serviceName: "WorkflowNodeStep",
      ...options.props,
    },
  });
  return wrapper;
};

describe("ConditionsEditor", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("rendering", () => {
    it("renders one ConditionRow per condition", () => {
      const wrapper = createWrapper();

      expect(wrapper.findAllComponents(ConditionRow)).toHaveLength(1);
    });

    it("renders multiple ConditionRows across sets", () => {
      const wrapper = createWrapper({ props: { modelValue: mockTwoConditionSets } });

      expect(wrapper.findAllComponents(ConditionRow)).toHaveLength(2);
    });

    it("renders multiple ConditionRows within a single set", () => {
      const wrapper = createWrapper({ props: { modelValue: mockSetWithTwoConditions } });

      expect(wrapper.findAllComponents(ConditionRow)).toHaveLength(2);
    });

    it("does not show OR separator for a single condition set", () => {
      const wrapper = createWrapper();

      expect(wrapper.find('[data-testid="or-separator"]').exists()).toBe(false);
    });

    it("shows OR separator between condition sets", () => {
      const wrapper = createWrapper({ props: { modelValue: mockTwoConditionSets } });

      expect(wrapper.find('[data-testid="or-separator"]').exists()).toBe(true);
    });

    it("does not show AND separator for a single condition in a set", () => {
      const wrapper = createWrapper();

      expect(wrapper.find('[data-testid="and-separator"]').exists()).toBe(false);
    });

    it("shows AND separator between conditions within a set", () => {
      const wrapper = createWrapper({ props: { modelValue: mockSetWithTwoConditions } });

      expect(wrapper.find('[data-testid="and-separator"]').exists()).toBe(true);
    });
  });

  describe("add condition", () => {
    it("shows add condition button when below max conditions per set", () => {
      const wrapper = createWrapper();

      expect(wrapper.find('[data-testid="add-condition-btn-0"]').exists()).toBe(true);
    });

    it("hides add condition button when at max conditions per set", () => {
      const wrapper = createWrapper({ props: { modelValue: mockSetAtMaxConditions } });

      expect(wrapper.find('[data-testid="add-condition-btn-0"]').exists()).toBe(false);
    });

    it("emits update:modelValue with a new condition appended when add condition is clicked", async () => {
      const wrapper = createWrapper();

      await wrapper.find('[data-testid="add-condition-btn-0"]').trigger("click");

      expect(wrapper.emitted("update:modelValue")).toBeTruthy();
      const emitted = wrapper.emitted("update:modelValue")![0][0] as any[];
      expect(emitted[0].conditions).toHaveLength(2);
      expect(emitted[0].conditions[1].field).toBeNull();
    });
  });

  describe("add condition set", () => {
    it("shows add condition set button when below max condition sets", () => {
      const wrapper = createWrapper();

      expect(wrapper.find('[data-testid="add-condition-set-btn"]').exists()).toBe(true);
    });

    it("hides add condition set button when at max condition sets", () => {
      const wrapper = createWrapper({ props: { modelValue: mockFiveConditionSets } });

      expect(wrapper.find('[data-testid="add-condition-set-btn"]').exists()).toBe(false);
    });

    it("emits update:modelValue with a new empty condition set when add set is clicked", async () => {
      const wrapper = createWrapper();

      await wrapper.find('[data-testid="add-condition-set-btn"]').trigger("click");

      expect(wrapper.emitted("update:modelValue")).toBeTruthy();
      const emitted = wrapper.emitted("update:modelValue")![0][0] as any[];
      expect(emitted).toHaveLength(2);
      expect(emitted[1].conditions).toHaveLength(1);
      expect(emitted[1].conditions[0].field).toBeNull();
    });
  });

  describe("condition interactions via child user actions", () => {
    it("emits update:modelValue with updated condition value when the value input changes", async () => {
      const wrapper = createWrapper();
      // Simulate PtAutoComplete emitting a new value to ConditionRow,
      // which then propagates up through ConditionRow → ConditionsEditor
      const ptAutoComplete = wrapper.findComponent(PtAutoComplete);
      await ptAutoComplete.vm.$emit("update:modelValue", "Windows");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("update:modelValue")).toBeTruthy();
      const emitted = wrapper.emitted("update:modelValue")![0][0] as any[];
      expect(emitted[0].conditions[0].value).toBe("Windows");
    });

    it("emits update:modelValue with the condition removed when the delete button is clicked", async () => {
      const wrapper = createWrapper({ props: { modelValue: mockSetWithTwoConditions } });
      // Both ConditionRows show the delete button because conditions.length > 1
      const deleteButtons = wrapper.findAll('[data-testid="condition-delete-btn"]');

      await deleteButtons[1].trigger("click");

      expect(wrapper.emitted("update:modelValue")).toBeTruthy();
      const emitted = wrapper.emitted("update:modelValue")![0][0] as any[];
      expect(emitted[0].conditions).toHaveLength(1);
    });

    it("does not show a delete button for single-condition sets even when multiple sets exist", () => {
      // showDeleteButton = conditionSet.conditions.length > 1 || conditionSet.length > 1
      // Inside the v-for, 'conditionSet' is the loop variable (a plain object, not the array),
      // so .length is always undefined and the second condition is always false.
      // Effective behavior: the delete button only appears when conditions.length > 1.
      const wrapper = createWrapper({ props: { modelValue: mockTwoConditionSets } });

      expect(wrapper.findAll('[data-testid="condition-delete-btn"]')).toHaveLength(0);
    });

    it("re-emits switch-step-type when the note link is clicked in the field dropdown", async () => {
      const wrapper = createWrapper({ props: { depth: 0 } });
      // Open the field PtSelect dropdown (pt-select-control falls through to the .p-select div)
      await wrapper.find('[data-testid="pt-select-control"]').trigger("click");
      await flushPromises();

      // The note link is rendered inside PrimeVue's teleported overlay in document.body
      const noteLink = document.body.querySelector('[data-testid="condition-note-link"]') as HTMLElement;
      expect(noteLink).not.toBeNull();
      noteLink.click();
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("switch-step-type")).toBeTruthy();
    });
  });

  describe("validation error display", () => {
    it("shows field error message via PtSelect when field error is set", () => {
      const conditionId = mockSingleConditionSet[0].conditions[0].id;
      const wrapper = createWrapper({
        props: {
          validation: {
            valid: false,
            errors: { conditions: { [conditionId]: { field: "editConditionalStep.fieldRequired" } } },
          },
        },
      });

      const errorEl = wrapper.find('[data-testid="pt-select-error"]');
      expect(errorEl.exists()).toBe(true);
      expect(errorEl.text()).toBe("editConditionalStep.fieldRequired");
    });

    it("shows value error message via PtAutoComplete when value error is set", () => {
      const conditionId = mockSingleConditionSet[0].conditions[0].id;
      const wrapper = createWrapper({
        props: {
          validation: {
            valid: false,
            errors: { conditions: { [conditionId]: { value: "editConditionalStep.valueRequired" } } },
          },
        },
      });

      const errorEl = wrapper.find('[data-testid="pt-autocomplete-error"]');
      expect(errorEl.exists()).toBe(true);
      expect(errorEl.text()).toBe("editConditionalStep.valueRequired");
    });

    it("does not render error messages when validation is clean", () => {
      const wrapper = createWrapper({
        props: {
          validation: { valid: true, errors: {} },
        },
      });

      expect(wrapper.find('[data-testid="pt-select-error"]').exists()).toBe(false);
      expect(wrapper.find('[data-testid="pt-autocomplete-error"]').exists()).toBe(false);
    });
  });

  describe("edge cases", () => {
    it("renders one ConditionRow even when modelValue is empty array", () => {
      const wrapper = createWrapper({ props: { modelValue: [] } });

      expect(wrapper.findAllComponents(ConditionRow)).toHaveLength(1);
    });
  });
});
