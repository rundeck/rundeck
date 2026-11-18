import { flushPromises, mount, VueWrapper } from "@vue/test-utils";
import EditProjectNodeSourceFile from "../EditProjectNodeSourceFile.vue";
import EditProjectNodeSourcePage from "../EditProjectNodeSourcePage.vue";
import {
  NodeSource,
  NodeSourceFile,
} from "../../../../library/stores/NodeSourceFile";

jest.mock("@/library/components/plugins/pluginConfig.vue", () => ({
  __esModule: true,
  default: { name: "PluginConfig", template: "<div></div>" },
}));

const eventBus = {
  on: jest.fn(),
  off: jest.fn(),
  emit: jest.fn(),
};

interface NodeSourceFileStub {
  index: number;
  content: string;
  modelFormat: string;
  nodeSource?: NodeSource;
  load: jest.Mock<Promise<void>, []>;
  retrieveSourceContent: jest.Mock<Promise<string>, []>;
  storeSourceContent: jest.Mock<Promise<string>, [string]>;
}

const nodeSourceFile: NodeSourceFileStub = {
  index: -1,
  content: "nodes: []",
  modelFormat: "yaml",
  nodeSource: undefined,
  load: jest.fn(),
  retrieveSourceContent: jest.fn(),
  storeSourceContent: jest.fn(),
};

const createWrapper = async (): Promise<VueWrapper> => {
  nodeSourceFile.load.mockImplementation(async () => {
    nodeSourceFile.nodeSource = {
      index: 0,
      type: "file",
      resources: {
        href: "/project/test/source/0/resources",
        description: "/project/etc/resources.yaml",
        syntaxMimeType: "application/yaml",
        writeable: true,
      },
    };
  });
  nodeSourceFile.retrieveSourceContent.mockResolvedValue("nodes: []");

  const wrapper = mount(EditProjectNodeSourcePage, {
    props: {
      index: 0,
      nextPageUrl: "/project/test/sources",
      eventBus,
    },
    global: {
      provide: {
        nodeSourceFile: nodeSourceFile as unknown as NodeSourceFile,
      },
      stubs: {
        PluginConfig: true,
        AceEditor: true,
        UiSocket: { template: "<div><slot /></div>" },
        PageConfirm: true,
      },
    },
  });
  await flushPromises();
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("EditProjectNodeSourcePage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    nodeSourceFile.index = -1;
    nodeSourceFile.nodeSource = undefined;
  });

  it("renders the node source editor", async () => {
    const wrapper = await createWrapper();

    expect(wrapper.findComponent(EditProjectNodeSourceFile).exists()).toBe(
      true,
    );
  });
});
