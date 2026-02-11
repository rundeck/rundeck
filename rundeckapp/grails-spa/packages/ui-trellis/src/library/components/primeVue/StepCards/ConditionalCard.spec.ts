import { shallowMount } from "@vue/test-utils";

jest.mock("@/library/modules/rundeckClient", () => ({
  client: jest.fn(),
}));

jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    eventBus: { on: jest.fn(), off: jest.fn(), emit: jest.fn() },
  }),
}));

jest.mock("@/library/stores/Plugins", () => ({
  ServiceType: {
    WorkflowNodeStep: "WorkflowNodeStep",
    WorkflowStep: "WorkflowStep",
  },
}));

jest.mock("@/app/components/job/workflow/stepEditorUtils", () => ({
  getPluginDetailsForStep: jest.fn().mockReturnValue({}),
}));

import ConditionalCard from "./ConditionalCard.vue";

const mockPluginDetails = {
  title: "Conditional Step",
  description: "A conditional step",
};

const mockConfig = {
  id: "conditional-1",
  type: "conditional.logic",
  config: {
    conditionSets: [{ id: "cs-1", conditions: [] }],
    commands: [],
  },
};

describe("ConditionalCard", () => {
  it("renders the component", () => {
    const wrapper = shallowMount(ConditionalCard, {
      props: {
        pluginDetails: mockPluginDetails,
        config: mockConfig,
      },
      global: {
        mocks: {
          $t: (key: string) => key,
        },
      },
    });
    expect(wrapper.exists()).toBe(true);
  });

  it("renders BaseStepCard", () => {
    const wrapper = shallowMount(ConditionalCard, {
      props: {
        pluginDetails: mockPluginDetails,
        config: mockConfig,
      },
      global: {
        mocks: {
          $t: (key: string) => key,
        },
      },
    });
    expect(wrapper.findComponent({ name: "BaseStepCard" }).exists()).toBe(true);
  });
});