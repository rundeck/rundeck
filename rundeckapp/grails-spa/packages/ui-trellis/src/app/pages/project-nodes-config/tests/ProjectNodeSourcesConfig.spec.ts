import { mount, flushPromises } from "@vue/test-utils";
import ProjectNodeSourcesConfig from "../ProjectNodeSourcesConfig.vue";
import { getProjectNodeSources } from "../nodeSourcesUtil";

jest.mock("../nodeSourcesUtil", () => ({
  getProjectNodeSources: jest.fn(),
}));
jest.mock("@/library", () => ({
  getRundeckContext: jest.fn().mockReturnValue({}),
}));
jest.mock("../ProjectPluginConfig.vue", () => ({
  __esModule: true,
  default: {
    name: "ProjectPluginConfig",
    template:
      '<div><slot name="item-extra" :plugin="{ origIndex: 0 }" mode="show" /></div>',
  },
}));

const mockedGetSources = getProjectNodeSources as jest.MockedFunction<
  typeof getProjectNodeSources
>;

const createWrapper = async (errors: string) => {
  mockedGetSources.mockResolvedValue([
    { index: 1, type: "file", errors, resources: { writeable: false } } as any,
  ]);
  const wrapper = mount(ProjectNodeSourcesConfig, {
    props: { eventBus: { emit: jest.fn() } as any },
  });
  await flushPromises();
  return wrapper;
};

describe("ProjectNodeSourcesConfig", () => {
  beforeAll(() => {
    window._rundeck = {
      rdBase: "http://localhost",
      projectName: "test-project",
    } as any;
  });

  afterAll(() => {
    Reflect.deleteProperty(window, "_rundeck");
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it("renders a multi-line error with the line-preserving class", async () => {
    const wrapper = await createWrapper("a\nb");
    const error = wrapper.find('[data-testid="node-source-error"]');

    expect(error.classes()).toContain("node-source-error");
    expect(error.element.textContent).toBe("a\nb");
  });

  it("escapes HTML in the error text", async () => {
    const wrapper = await createWrapper("<b>x</b>");
    const error = wrapper.find('[data-testid="node-source-error"]');

    expect(error.text()).toBe("<b>x</b>");
    expect(error.element.querySelector("b")).toBeNull();
  });
});
