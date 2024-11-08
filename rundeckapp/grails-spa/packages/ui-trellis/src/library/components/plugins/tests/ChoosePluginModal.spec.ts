import { shallowMount, VueWrapper, flushPromises } from "@vue/test-utils";
import ChoosePluginModal from "../ChoosePluginModal.vue";
import { Modal, Btn, Tabs, Tab } from "uiv";
const mockServices = [
  {
    service: "mockService1",
    providers: [{ name: "provider1" }, { name: "provider2" }],
  },
  {
    service: "mockService2",
    providers: [{ name: "provider3" }],
  },
];
jest.mock("@/library", () => ({
  getRundeckContext: jest.fn(() => ({
    rootStore: {
      plugins: {
        load: jest.fn(),
        getServicePlugins: jest.fn((service) =>
          service === "mockService1"
            ? [{ name: "provider1" }, { name: "provider2" }]
            : [{ name: "provider3" }],
        ),
      },
    },
  })),
}));
const createWrapper = async (props = {}): Promise<VueWrapper<any>> => {
  const wrapper = shallowMount(ChoosePluginModal, {
    props: {
      title: "Test Title",
      services: mockServices.map((s) => s.service),
      modelValue: true,
      tabNames: ["Service", "Provider"],
      ...props,
    },
    global: {
      components: { Modal, Btn, Tabs, Tab },
      mocks: {
        $t: (msg) => msg,
      },
      stubs: {
        Modal: false,
        Btn: false,
        Tabs: false,
        Tab: false,
      },
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
    await flushPromises();
    const providerButtons = wrapper.findAll('[data-testid="provider-button"]');
    expect(providerButtons.length).toBe(3);
    await providerButtons[0].trigger("click");
    expect(wrapper.emitted("selected")).toBeTruthy();
    expect(wrapper.emitted("selected")[0][0]).toEqual({
      service: "mockService1",
      provider: "provider1",
    });
  });
  it('emits "cancel" event when cancel button is clicked', async () => {
    const wrapper = await createWrapper();
    await flushPromises();
    const cancelButton = wrapper.find('[data-testid="cancel-button"]');
    expect(wrapper.text()).toContain("Cancel");
    await cancelButton.trigger("click");
    expect(wrapper.emitted("cancel")).toBeTruthy();
    expect(wrapper.emitted("cancel").length).toBe(1);
  });
});
