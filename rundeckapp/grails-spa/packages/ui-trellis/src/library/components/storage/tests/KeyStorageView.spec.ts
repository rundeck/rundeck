import { mount } from "@vue/test-utils";
import KeyStorageView from "../KeyStorageView.vue";
import { Modal } from "uiv";

jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    rdBase: "mockRdBase",
    projectName: "test-project",
    eventBus: {
      on: jest.fn(),
      emit: jest.fn(),
      off: jest.fn(),
    },
    rundeckClient: {
      storageKeyGetMetadata: jest.fn().mockResolvedValue({
        _response: { status: 200 },
        resources: [{ name: "key1", path: "/keys/key1", type: "publicKey" }],
      }),
      storageKeyDelete: jest.fn().mockResolvedValue({
        _response: { status: 200 },
      }),
      projectList: jest.fn().mockResolvedValue([]),
    },
  }),
  url: jest.fn().mockReturnValue({ href: "mockHref" }),
}));
const mountKeyStorageView = async (props = ({} = {})) => {
  return mount(KeyStorageView, {
    props: {
      rootPath: "/keys",
      readOnly: false,
      allowUpload: true,
      ...props,
    },
    global: {
      components: {
        Modal,
      },
      mocks: {
        $t: (msg) => msg,
      },
    },
    data() {
      return {
        isSelectedKey: true,
        selectedKey: {
          name: "key1",
          path: "/keys/key1",
          meta: {
            rundeckKeyType: "private",
          },
        },
      };
    },
  });
};
describe("KeyStorageView", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });
  it("emits openEditor when the Add or Upload a Key button is clicked", async () => {
    const wrapper = await mountKeyStorageView();
    const addButton = wrapper.find('button[data-testid="add-key-btn"]');
    await addButton.trigger("click");
    expect(wrapper.emitted().openEditor).toBeTruthy();
  });
  it("emits openEditor when the Overwrite Key button is clicked", async () => {
    const wrapper = await mountKeyStorageView();
    await wrapper.vm.$nextTick();
    const overwriteButton = wrapper.find(
      'button[data-testid="overwrite-key-btn"]',
    );
    expect(overwriteButton.exists()).toBe(true);
    await overwriteButton.trigger("click");
    expect(wrapper.emitted().openEditor).toBeTruthy();
  });
  it("opens the confirmation modal when the Delete button is clicked", async () => {
    const wrapper = await mountKeyStorageView();
    const deleteButton = wrapper.find('button[data-testid="delete-key-btn"]');
    await deleteButton.trigger("click");
    const modal = wrapper.findComponent(Modal);
    expect(modal.exists()).toBe(true);
  });
});
