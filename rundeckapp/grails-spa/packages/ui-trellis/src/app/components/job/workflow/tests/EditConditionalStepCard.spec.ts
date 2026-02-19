import { mount } from "@vue/test-utils";
import EditConditionalStepCard from "../EditConditionalStepCard.vue";
import InnerStepList from "../InnerStepList.vue";
import ConditionsEditor from "../ConditionsEditor.vue";
import type { EditStepData } from "../types/workflowTypes";

jest.mock("vue-scrollto", () => ({ scrollTo: jest.fn() }));
jest.mock("@/library/modules/rundeckClient", () => ({ client: jest.fn() }));

jest.mock("@/library/stores/Plugins", () => ({
  ServiceType: {
    WorkflowNodeStep: "WorkflowNodeStep",
    WorkflowStep: "WorkflowStep",
  },
}));

jest.mock("@/library/stores/contextVariables", () => ({
  contextVariables: jest.fn().mockReturnValue({ job: [], node: [] }),
}));

jest.mock("@/app/utilities/loadJsonData", () => ({
  loadJsonData: jest.fn().mockReturnValue(null),
}));

jest.mock("@/library/rundeckService.ts", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    eventBus: { on: jest.fn(), emit: jest.fn() },
  }),
}));

const baseModel: EditStepData = {
  type: "conditional.logic",
  config: { conditionSet: [], subSteps: [] },
  nodeStep: true,
  id: "step-1",
};

const modelWithSubStep: EditStepData = {
  type: "conditional.logic",
  description: "My conditional",
  config: {
    conditionSet: [],
    subSteps: [
      { type: "exec-command", config: { adhocRemoteString: "echo hello" }, nodeStep: true, id: "sub-1" },
    ],
  },
  nodeStep: true,
  id: "step-1",
};

const innerStepListStub = {
  name: "InnerStepList",
  template: `<div data-testid="inner-step-list-stub"></div>`,
  props: ["modelValue", "targetService", "depth", "extraAutocompleteVars"],
  emits: ["update:modelValue", "update:editing"],
};

let wrapper: ReturnType<typeof mount>;

afterEach(() => {
  wrapper?.unmount();
});

const createWrapper = (options: { props?: Record<string, any> } = {}) => {
  wrapper = mount(EditConditionalStepCard, {
    props: {
      modelValue: baseModel,
      serviceName: "WorkflowNodeStep",
      ...options.props,
    },
    global: {
      stubs: { InnerStepList: innerStepListStub },
    },
  });
  return wrapper;
};

describe("EditConditionalStepCard", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("cancel and close", () => {
    it("emits cancel when close button is clicked", async () => {
      const wrapper = createWrapper();

      await wrapper.find('[data-testid="close-btn"]').trigger("click");

      expect(wrapper.emitted("cancel")).toBeTruthy();
    });

    it("emits cancel when cancel button is clicked", async () => {
      const wrapper = createWrapper();

      await wrapper.find('[data-testid="cancel-btn"]').trigger("click");

      expect(wrapper.emitted("cancel")).toBeTruthy();
    });
  });

  describe("save button state", () => {
    it("save button is disabled when there are no inner commands", () => {
      const wrapper = createWrapper({ props: { modelValue: baseModel } });

      expect(wrapper.find('[data-testid="save-btn"]').attributes("disabled")).toBeDefined();
    });

    it("save button is enabled when inner commands exist", () => {
      const wrapper = createWrapper({ props: { modelValue: modelWithSubStep } });

      expect(wrapper.find('[data-testid="save-btn"]').attributes("disabled")).toBeUndefined();
    });

    it("save button becomes disabled when InnerStepList signals editing started", async () => {
      const wrapper = createWrapper({ props: { modelValue: modelWithSubStep } });
      const innerStepList = wrapper.findComponent(InnerStepList);

      // InnerStepList is stubbed — vm.$emit is the only way to trigger its editing state
      await innerStepList.vm.$emit("update:editing", true);
      await wrapper.vm.$nextTick();

      expect(wrapper.find('[data-testid="save-btn"]').attributes("disabled")).toBeDefined();
    });

    it("save button becomes enabled again when InnerStepList signals editing stopped", async () => {
      const wrapper = createWrapper({ props: { modelValue: modelWithSubStep } });
      const innerStepList = wrapper.findComponent(InnerStepList);

      await innerStepList.vm.$emit("update:editing", true);
      await wrapper.vm.$nextTick();
      await innerStepList.vm.$emit("update:editing", false);
      await wrapper.vm.$nextTick();

      expect(wrapper.find('[data-testid="save-btn"]').attributes("disabled")).toBeUndefined();
    });
  });

  describe("save payload", () => {
    it("emits save and update:modelValue when save button is clicked", async () => {
      const wrapper = createWrapper({ props: { modelValue: modelWithSubStep } });

      await wrapper.find('[data-testid="save-btn"]').trigger("click");

      expect(wrapper.emitted("save")).toBeTruthy();
      expect(wrapper.emitted("update:modelValue")).toBeTruthy();
    });

    it("includes the typed step name in the save payload", async () => {
      const wrapper = createWrapper({ props: { modelValue: modelWithSubStep } });

      // Type into the real PtInput — data-testid falls through InputText → <input>
      await wrapper.find('[data-testid="pt-input-field"]').setValue("Renamed Step");
      await wrapper.find('[data-testid="save-btn"]').trigger("click");

      const emitted = wrapper.emitted("update:modelValue")![0][0] as EditStepData;
      expect(emitted.description).toBe("Renamed Step");
    });

    it("includes an updated conditionSet when a condition is added via ConditionsEditor", async () => {
      const wrapper = createWrapper({ props: { modelValue: modelWithSubStep } });

      // Click 'add condition' in the real ConditionsEditor — emits update:modelValue upward
      await wrapper.find('[data-testid="add-condition-btn-0"]').trigger("click");
      await wrapper.find('[data-testid="save-btn"]').trigger("click");

      const emitted = wrapper.emitted("update:modelValue")![0][0] as EditStepData;
      expect(emitted.config?.conditionSet?.[0].conditions).toHaveLength(2);
    });

    it("includes the current subSteps in the save payload", async () => {
      const wrapper = createWrapper({ props: { modelValue: modelWithSubStep } });

      await wrapper.find('[data-testid="save-btn"]').trigger("click");

      const emitted = wrapper.emitted("update:modelValue")![0][0] as EditStepData;
      expect(emitted.config?.subSteps).toHaveLength(1);
    });
  });

  describe("card title", () => {
    it("shows the node step title when serviceName is WorkflowNodeStep", () => {
      const wrapper = createWrapper({ props: { serviceName: "WorkflowNodeStep" } });

      expect(wrapper.find('[data-testid="card-title"]').text()).toBe("editConditionalStep.title");
    });

    it("shows the workflow step title when serviceName is WorkflowStep", () => {
      const wrapper = createWrapper({ props: { serviceName: "WorkflowStep" } });

      expect(wrapper.find('[data-testid="card-title"]').text()).toBe("editConditionalStep.titleWorkflow");
    });
  });

  describe("initialization from modelValue", () => {
    it("pre-fills the step name input from modelValue.description", () => {
      const wrapper = createWrapper({ props: { modelValue: modelWithSubStep } });

      expect((wrapper.find('[data-testid="pt-input-field"]').element as HTMLInputElement).value).toBe("My conditional");
    });
  });

  describe("switch-step-type", () => {
    it("re-emits switch-step-type when ConditionsEditor emits it", async () => {
      // The full dropdown interaction (open PtSelect → click note link → emit switch-step-type)
      // is covered at the ConditionRow and ConditionsEditor levels. Here we verify that
      // EditConditionalStepCard correctly propagates the event upward from ConditionsEditor.
      const wrapper = createWrapper();
      const conditionsEditor = wrapper.findComponent(ConditionsEditor);

      await conditionsEditor.vm.$emit("switch-step-type");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("switch-step-type")).toBeTruthy();
    });
  });
});
