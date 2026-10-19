import { mount, flushPromises } from "@vue/test-utils";
import axios from "axios";
import ProjectPluginConfig from "../ProjectPluginConfig.vue";

jest.mock("axios");
jest.mock("@/library/modules/pluginService", () => ({
  __esModule: true,
  default: {
    getPluginProvidersForService: jest
      .fn()
      .mockResolvedValue({ service: false }),
    validatePluginConfig: jest.fn(),
  },
}));
jest.mock("@/library/components/plugins/pluginConfig.vue", () => ({
  __esModule: true,
  default: {
    name: "PluginConfig",
    template: '<div class="plugin-config-stub"></div>',
  },
}));
jest.mock("@/library/services/api", () => ({
  api: {
    get: jest.fn(),
    post: jest.fn(),
  },
}));

const mockedAxios = axios as unknown as jest.Mock;

const setPluginResponse = (plugins: Record<string, any>[]) => {
  mockedAxios.mockResolvedValue({
    status: 200,
    data: { plugins },
  });
};

const createWrapper = async (props: Record<string, any> = {}) => {
  const wrapper = mount(ProjectPluginConfig, {
    props: {
      serviceName: "ResourceModelSource",
      configPrefix: "resources.source",
      ...props,
    },
    global: {
      stubs: {
        PluginInfo: true,
        UiSocket: true,
        Expandable: true,
      },
    },
  });
  await flushPromises();
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("ProjectPluginConfig", () => {
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

  describe("rendering plugin config entries", () => {
    it("renders a list item for each configured plugin", async () => {
      setPluginResponse([
        { type: "aws-ec2-nodes", config: {} },
        { type: "aws-ec2-nodes", config: {} },
      ]);
      const wrapper = await createWrapper();

      expect(wrapper.find('[data-testid="node-source-item-0"]').exists()).toBe(
        true,
      );
      expect(wrapper.find('[data-testid="node-source-item-1"]').exists()).toBe(
        true,
      );
    });

    it("renders no list items when there are no configured plugins", async () => {
      setPluginResponse([]);
      const wrapper = await createWrapper();

      expect(wrapper.find('[data-testid="node-source-item-0"]').exists()).toBe(
        false,
      );
    });
  });

  describe("disabled plugin entries", () => {
    it("adds the disabled class when config.disabled is boolean true", async () => {
      setPluginResponse([
        { type: "aws-ec2-nodes", config: { disabled: true } },
      ]);
      const wrapper = await createWrapper();

      expect(
        wrapper.find('[data-testid="node-source-item-0"]').classes(),
      ).toContain("disabled");
    });

    it('adds the disabled class when config.disabled is the string "true"', async () => {
      setPluginResponse([
        { type: "aws-ec2-nodes", config: { disabled: "true" } },
      ]);
      const wrapper = await createWrapper();

      expect(
        wrapper.find('[data-testid="node-source-item-0"]').classes(),
      ).toContain("disabled");
    });

    it("does not add the disabled class when config.disabled is boolean false", async () => {
      setPluginResponse([
        { type: "aws-ec2-nodes", config: { disabled: false } },
      ]);
      const wrapper = await createWrapper();

      expect(
        wrapper.find('[data-testid="node-source-item-0"]').classes(),
      ).not.toContain("disabled");
    });

    it('does not add the disabled class when config.disabled is the string "false"', async () => {
      setPluginResponse([
        { type: "aws-ec2-nodes", config: { disabled: "false" } },
      ]);
      const wrapper = await createWrapper();

      expect(
        wrapper.find('[data-testid="node-source-item-0"]').classes(),
      ).not.toContain("disabled");
    });

    it("does not add the disabled class when config.disabled is not set", async () => {
      setPluginResponse([{ type: "aws-ec2-nodes", config: {} }]);
      const wrapper = await createWrapper();

      expect(
        wrapper.find('[data-testid="node-source-item-0"]').classes(),
      ).not.toContain("disabled");
    });
  });
});
