import { mount, VueWrapper } from "@vue/test-utils";
import EditProjectNodeSourceFile from "../EditProjectNodeSourceFile.vue";

jest.mock("@/library/components/plugins/pluginConfig.vue", () => ({
  __esModule: true,
  default: { name: "PluginConfig", template: "<div></div>" },
}));

interface MountOptions {
  pathOutsideAllowedPaths?: boolean;
}

const eventBus = {
  on: jest.fn(),
  off: jest.fn(),
  emit: jest.fn(),
};

const createWrapper = async (
  options: MountOptions = {},
): Promise<VueWrapper> => {
  const wrapper = mount(EditProjectNodeSourceFile, {
    props: {
      eventBus,
      index: 0,
      sourceDesc: "/project/etc/resources.yaml",
      fileFormat: "yaml",
      provider: "file",
      value: "nodes: []",
      pathOutsideAllowedPaths: options.pathOutsideAllowedPaths ?? false,
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

  it("shows remediation guidance for a source outside allowed paths", async () => {
    const wrapper = await createWrapper({ pathOutsideAllowedPaths: true });
    const warning = wrapper.find('[data-testid="node-source-path-warning"]');

    expect(warning.exists()).toBe(true);
    expect(warning.text()).toBe(
      "project.nodes.edit.file.path.outside.allowed.warning",
    );
  });

  it("does not show the warning for a source within allowed paths", async () => {
    const wrapper = await createWrapper();

    expect(
      wrapper.find('[data-testid="node-source-path-warning"]').exists(),
    ).toBe(false);
  });
});
