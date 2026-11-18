import { mount, VueWrapper } from "@vue/test-utils";
import EditProjectNodeSourceFile from "../EditProjectNodeSourceFile.vue";
import messages from "../../../utilities/locales/en_US.js";

const $t = (key: string) => (messages as Record<string, string>)[key] || key;

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
      mocks: { $t },
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
      "This node source uses a file outside the project's allowed paths. " +
        "Move the file into the project directory or add its base directory " +
        "to rundeck.resourceModelSource.file.allowedBasePaths.",
    );
  });

  it("does not show the warning for a source within allowed paths", async () => {
    const wrapper = await createWrapper();

    expect(
      wrapper.find('[data-testid="node-source-path-warning"]').exists(),
    ).toBe(false);
  });
});
