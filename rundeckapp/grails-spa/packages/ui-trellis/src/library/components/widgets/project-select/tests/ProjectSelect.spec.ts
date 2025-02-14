import { mount, VueWrapper, flushPromises } from "@vue/test-utils";
import ProjectSelect from "../ProjectSelect.vue";
import { EventBus } from "../../../../utilities/vueEventBus";

jest.mock("../../../../rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({}),

  getAppLinks: jest.fn(() => ({})),
  url: jest.fn().mockReturnValue("http://localhost"),
}));
jest.mock("../../../../stores/RootStore", () => ({
  RootStore: jest.fn().mockImplementation(() => ({
    projects: {
      loaded: true,
      projects: [
        { name: "Project A", label: "Project A Label" },
        { name: "Project B", label: "Project B Label" },
      ],
      search: jest.fn((term: string) =>
        term
          ? [{ name: "Project A", label: "Project A Label" }]
          : [
              { name: "Project A", label: "Project A Label" },
              { name: "Project B", label: "Project B Label" },
            ],
      ),
      load: jest.fn(),
    },
  })),
}));

const createWrapper = async (props = {}): Promise<VueWrapper<any>> => {
  const wrapper = mount(ProjectSelect, {
    props: {
      showButtons: true,
      mode: "multi",
      selectedProjects: [],
      ...props,
    },
  });
  await flushPromises();
  return wrapper;
};

describe("ProjectSelect.vue", () => {
  beforeAll(() => {
    window._rundeck = {
      rdBase: "http://localhost",
      apiVersion: "mockApiVersion",
      projectName: "test-project",
      activeTour: "mockActiveTour",
      activeTourStep: "mockActiveTourStep",
      appMeta: {},
      token: { TOKEN: "mockToken", URI: "mockUri" },
      tokens: {
        mockToken1: { TOKEN: "mockToken1", URI: "mockUri1" },
        mockToken2: { TOKEN: "mockToken2", URI: "mockUri2" },
      },
      navbar: { items: [] },
      feature: {},
      data: {},
      eventBus: EventBus,
      rundeckClient: {} as any,
      rootStore: new (jest.requireMock(
        "../../../../stores/RootStore",
      ).RootStore)(),
    };
  });
  afterAll(() => {
    delete window._rundeck;
  });
  afterEach(() => {
    jest.clearAllMocks();
  });

  it("allows selecting 'Select All' when projects are available", async () => {
    const wrapper = await createWrapper();
    await flushPromises();
    const selectAllCheckbox = wrapper.get(
      'input[type="checkbox"][value="_all"]',
    );
    await selectAllCheckbox.trigger("click");
    const emittedEvents = wrapper.emitted("update:selection");
    expect(emittedEvents?.length).toBeGreaterThan(0);
    expect(emittedEvents[0]).toEqual([["Project A", "Project B"]]);
  });
  it("allows deselecting 'Select All'", async () => {
    const wrapper = await createWrapper({
      mode: "multi",
      selectedProjects: ["Project A", "Project B"],
    });
    await flushPromises();
    const selectAllCheckbox = wrapper.get(
      'input[type="checkbox"][value="_all"]',
    );
    await selectAllCheckbox.trigger("click");
    await flushPromises();
    const emittedEvents = wrapper.emitted("update:selection");
    expect(emittedEvents).toBeTruthy();
    expect(emittedEvents[emittedEvents.length - 1]).toEqual([[]]);
  });

  it("handles 'Select All' visibility correctly", async () => {
    //  At least one project available,"Select All" should be visible
    let wrapper = await createWrapper();
    await flushPromises();
    let selectAllOption = wrapper.find('[data-testid="select-all-option"]');
    expect(selectAllOption.text()).toBe("select.all");
    // No projects available,"Select All" should NOT be visible
    window._rundeck.rootStore.projects.projects = [];
    wrapper = await createWrapper();
    await flushPromises();
    expect(wrapper.findAll('[data-testid="select-all-option"]').length).toBe(0);
  });
});
