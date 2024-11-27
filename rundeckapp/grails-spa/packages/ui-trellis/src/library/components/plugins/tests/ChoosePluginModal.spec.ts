import { mount, VueWrapper, flushPromises } from "@vue/test-utils";
import ChoosePluginModal from "../ChoosePluginModal.vue";
import PluginSearch from "../PluginSearch.vue";
import { Popover, Tabs, Tab } from "uiv";

jest.mock("@/library", () => ({
  getRundeckContext: jest.fn(() => ({
    rootStore: {
      plugins: {
        load: jest.fn(),
        getServicePlugins: jest.fn((service) =>
          service === "WorkflowStep"
            ? [
                {
                  name: "provider1",
                  title: "plugin1",
                  description: "done by John Doe",
                  isHighlighted: true,
                  highlightedOrder: 0,
                },
                {
                  name: "provider2",
                  title: "plugin2",
                  description: "done by Jane Doe",
                  isHighlighted: false,
                  highlightedOrder: 0,
                },
              ]
            : [
                {
                  name: "provider3",
                  title: "plugin3",
                  description: "run a script locally",
                  isHighlighted: false,
                },
              ],
        ),
      },
    },
  })),
}));

const createWrapper = async (props = {}): Promise<VueWrapper<any>> => {
  const wrapper = mount(ChoosePluginModal, {
    props: {
      title: "Test Title",
      services: ["WorkflowStep", "mockService2"],
      modelValue: true,
      tabNames: ["Service", "Provider"],
      ...props,
    },
    global: {
      components: { Tabs, Tab, Popover },
    },
    attachTo: document.body,
  });
  await flushPromises();
  return wrapper;
};

describe("ChoosePluginModal", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('emits "selected" event with correct data when a provider button is clicked', async () => {
    const wrapper = await createWrapper();
    await wrapper.find('[data-test="provider-button"]').trigger("click");
    expect(wrapper.emitted("selected")[0]).toEqual([
      {
        provider: "provider1",
        service: "WorkflowStep",
      },
    ]);
  });

  it('emits "cancel" event when cancel button is clicked', async () => {
    const wrapper = await createWrapper();
    const cancelButton = wrapper.find('[data-testid="cancel-button"]');
    await cancelButton.trigger("click");
    expect(wrapper.emitted("cancel")).toBeTruthy();
  });

  it("renders a list when there is a single service", async () => {
    const wrapper = await createWrapper({ services: ["WorkflowStep"] });
    expect(wrapper.findComponent(Tabs).exists()).toBeFalsy();
    expect(wrapper.find('[data-testid="list-view"]').exists()).toBeTruthy();
  });

  it("renders an input for search when showSearch is true and correctly handles searches", async () => {
    const wrapper = await createWrapper({ showSearch: true });
    const searchComponent = wrapper.findComponent(PluginSearch);
    expect(searchComponent.exists()).toBeTruthy();
    searchComponent.vm.$emit("search", "plugin2");
    await wrapper.vm.$nextTick();

    expect(wrapper.findAll("[data-test='provider-button']").length).toBe(1);
    expect(wrapper.find("[data-test='provider-button']").text()).toBe(
      "plugin2 - done by Jane Doe",
    );
    // when user erases the search query, reset results back to original
    searchComponent.vm.$emit("search", "");
    await wrapper.vm.$nextTick();

    const tabsComponent = wrapper.findComponent(Tabs);
    const tabTitles = tabsComponent.findAll('[role="tab"]');
    expect(tabTitles[0].text()).toBe("Service (2)");
    expect(tabTitles[1].text()).toBe("Provider (1)");
    expect(wrapper.findAll("[data-test='provider-button']").length).toBe(3);
  });

  it("renders dividers if showDividers is true and dividerIndex for a service is different than 0", async () => {
    const wrapper = await createWrapper({ showDivider: true });
    const divider = wrapper.findAll('[data-testid="divider"]');
    expect(divider.length).toBe(1);
    expect(divider[0].text()).toBe("workflow.step.plugin.plural");
  });
});
