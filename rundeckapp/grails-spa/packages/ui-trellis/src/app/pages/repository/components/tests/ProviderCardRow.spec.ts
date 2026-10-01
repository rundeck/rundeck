import { mount, VueWrapper } from "@vue/test-utils";
import { createStore } from "vuex";
import ProviderCardRow from "../ProviderCardRow.vue";

interface MountOptions {
  props?: Record<string, any>;
  state?: Record<string, any>;
}

const createTestStore = (state: Record<string, any> = {}) =>
  createStore({
    modules: {
      plugins: {
        namespaced: true,
        state: () => ({
          selectedServiceFacet: null,
          ...state,
        }),
        actions: {
          getProviderInfo: jest.fn(),
          uninstallPlugin: jest.fn(),
        },
      },
    },
  });

const createWrapper = async (
  options: MountOptions = {},
): Promise<VueWrapper<any>> => {
  const wrapper = mount(ProviderCardRow, {
    props: {
      provider: {
        name: "test-provider",
        service: "WorkflowStep",
        builtin: false,
        pluginVersion: "1.0.0",
      },
      ...options.props,
    },
    global: {
      plugins: [createTestStore(options.state)],
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

const findByTestId = (wrapper: VueWrapper<any>, testId: string) =>
  wrapper.find(`[data-testid="${testId}"]`);

describe("ProviderCardRow", () => {
  // RUN-4966: the Uninstall button had no Bootstrap color variant class,
  // rendering as unstyled native browser chrome in both light and dark mode.
  it("renders the uninstall button with the btn-default variant class", async () => {
    const wrapper = await createWrapper();

    const uninstallButton = findByTestId(
      wrapper,
      "provider-card-row-uninstall-button",
    );

    expect(uninstallButton.exists()).toBe(true);
    expect(uninstallButton.classes()).toContain("btn-default");
    expect(uninstallButton.classes()).toContain("btn");
    expect(uninstallButton.classes()).toContain("btn-sm");
    expect(uninstallButton.classes()).toContain("btn-block");
  });

  it("does not render the uninstall button for a builtin provider", async () => {
    const wrapper = await createWrapper({
      props: {
        provider: {
          name: "builtin-provider",
          service: "WorkflowStep",
          builtin: true,
          pluginVersion: "1.0.0",
        },
      },
    });

    expect(
      findByTestId(wrapper, "provider-card-row-uninstall-button").exists(),
    ).toBe(false);
  });
});
