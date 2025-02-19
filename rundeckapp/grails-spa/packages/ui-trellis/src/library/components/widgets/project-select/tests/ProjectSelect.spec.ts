import { mount, VueWrapper, flushPromises } from "@vue/test-utils";
import ProjectSelect from "../ProjectSelect.vue";
import { EventBus } from "../../../../utilities/vueEventBus";

jest.mock("../../../../rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({}),

  getAppLinks: jest.fn(() => ({
    menuHome: "http://localhost",
  })),
  url: jest.fn((path: string) => ({ href: `http://localhost${path}` })),

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

  it("typing in the search input filters the projects", async () => {
    const wrapper = await createWrapper();
    const searchInput = wrapper.find('[data-testid="search-projects"]');
    expect(searchInput.exists()).toBe(true);
    await searchInput.setValue("Project A");
    await flushPromises();
    const projectItems = wrapper.findAll(".scroller__item");
    expect(projectItems).toHaveLength(1);
    expect(projectItems[0].text()).toContain("Project A Label");
  });
  it("navigates to correct URL when clicking on a project in single mode", async () => {
    const wrapper = await createWrapper({ mode: "single" });
    await flushPromises();
    const projectLink = wrapper.findAll(".scroller__item");
    await flushPromises();
    expect(projectLink).toHaveLength(2);
    expect(projectLink[0].attributes("href")).toBe(
      "http://localhost?project=Project A",
    );
  });
  it("emits correct selection updates when selecting projects in multi mode", async () => {
    const wrapper = await createWrapper({ mode: "multi" });
    await flushPromises();
    const firstProjectCheckbox = wrapper.get(
      '[data-testid="projectCheckbox-Project A"]',
    );
    await firstProjectCheckbox.trigger("click");
    await flushPromises();
    const emittedEvents = wrapper.emitted("update:selection");
    expect(emittedEvents).toBeTruthy();
    expect(emittedEvents[0]).toEqual([["Project A"]]);
  });
  it("navigates correctly when clicking 'View All' and 'Click Project' button", async () => {
    const wrapper = await createWrapper();
    await flushPromises();
    const viewAllButton = wrapper.find('[data-testid="view-all-button"]');
    expect(viewAllButton.attributes("href")).toBe("http://localhost");
    const createProjectButton = wrapper.find(
      '[data-testid="create-project-button"]',
    );
    expect(createProjectButton.attributes("href")).toBe(
      "http://localhostresources/createProject",
    );

  });
});
