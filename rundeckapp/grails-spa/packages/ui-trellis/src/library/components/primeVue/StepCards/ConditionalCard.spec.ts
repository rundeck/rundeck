import { shallowMount } from "@vue/test-utils";
import ConditionalCard from "./ConditionalCard.vue";

// Mock rundeckClient to avoid import errors
jest.mock("@/library/modules/rundeckClient", () => ({
  client: {},
}));

// Mock api service
jest.mock("@/library/services/api", () => ({
  api: {
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
    delete: jest.fn(),
  },
}));

// Mock getRundeckContext
jest.mock("@/library", () => ({
  getRundeckContext: () => ({
    apiVersion: "44",
    rdBase: "/",
    eventBus: {
      emit: jest.fn(),
      on: jest.fn(),
      off: jest.fn(),
    },
  }),
}));

const createWrapper = async (props = {}) => {
  const defaultProps = {
    pluginDetails: {
      name: "test-plugin",
      title: "Test Plugin",
      iconUrl: "/test-icon.png",
    },
    config: {
      id: "test-id",
      type: "test-type",
    },
    ...props,
  };

  const wrapper = shallowMount(ConditionalCard, {
    props: defaultProps,
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("ConditionalCard", () => {
  describe("Rendering", () => {
    it("renders the component", async () => {
      const wrapper = await createWrapper();
      expect(wrapper.exists()).toBe(true);
    });

    it("applies complex class when complex prop is true", async () => {
      const wrapper = await createWrapper({ complex: true });
      
      const card = wrapper.find(".conditionalCard");
      expect(card.classes()).toContain("complex");
    });

    it("does not apply complex class when complex prop is false", async () => {
      const wrapper = await createWrapper({ complex: false });
      
      const card = wrapper.find(".conditionalCard");
      expect(card.classes()).not.toContain("complex");
    });
  });
});
