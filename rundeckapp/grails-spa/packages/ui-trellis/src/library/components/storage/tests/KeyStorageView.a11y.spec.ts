import { mount, VueWrapper } from "@vue/test-utils";
import KeyStorageView from "../KeyStorageView.vue";

jest.mock("../../../services/projects", () => ({
  listProjects: jest.fn().mockResolvedValue([]),
}));

jest.mock("../../../services/storage", () => ({
  storageKeyDelete: jest.fn().mockResolvedValue(true),
  storageKeyGetMetadata: jest.fn().mockResolvedValue({ resources: [] }),
}));

interface MountOptions {
  props?: Record<string, unknown>;
}

const createWrapper = async (
  options: MountOptions = {},
): Promise<VueWrapper<any>> => {
  const wrapper = mount(KeyStorageView, {
    props: {
      rootPath: "",
      readOnly: false,
      allowUpload: true,
      ...options.props,
    },
    data() {
      return {
        files: [
          {
            name: "/myKey",
            path: "/keys/myKey",
            type: "file",
            meta: { "Rundeck-key-type": "private" },
          },
        ],
        isSelectedKey: true,
        selectedKey: {
          name: "/myKey",
          path: "/keys/myKey",
          type: "file",
          meta: { "Rundeck-key-type": "private" },
        },
      };
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("KeyStorageView accessibility", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it("uses the localized storage path label for the storage path link", async () => {
    const wrapper = await createWrapper();

    expect(
      wrapper
        .find('[data-testid="storage-path-link"]')
        .attributes("aria-label"),
    ).toBe("storage.path.link.aria.label");
  });
});
