import { mount, VueWrapper } from "@vue/test-utils";
import EditProjectNodeSourceFile from "../EditProjectNodeSourceFile.vue";
jest.mock("@/library/components/plugins/pluginConfig.vue", () => ({
  __esModule: true,
  default: { name: "PluginConfig", template: "<div></div>" },
}));

const eventBus = {
  on: jest.fn(),
  off: jest.fn(),
  emit: jest.fn(),
};

const createWrapper = async (): Promise<VueWrapper> => {
  const wrapper = mount(EditProjectNodeSourceFile, {
    props: {
      eventBus,
      index: 0,
      sourceDesc: "/project/etc/resources.yaml",
      fileFormat: "yaml",
      provider: "file",
      value: "nodes: []",
    },
    global: {
      stubs: {
        PluginConfig: true,
        AceEditor: true,
        UiSocket: { template: "<div><slot /></div>" },
        PageConfirm: true,
      },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("EditProjectNodeSourceFile", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("does not show the path warning in the file editor", async () => {
    const wrapper = await createWrapper();

    expect(
      wrapper.find('[data-testid="node-source-path-warning"]').exists(),
    ).toBe(false);
  });
});
