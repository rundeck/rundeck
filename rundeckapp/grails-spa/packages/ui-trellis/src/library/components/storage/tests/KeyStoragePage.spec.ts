import { mount, VueWrapper } from "@vue/test-utils";
import { ComponentPublicInstance } from "vue";
import KeyStoragePage from "../KeyStoragePage.vue";
import { Modal } from "uiv";

jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    rdBase: "mockRdBase",
    projectName: "test-project",
    rundeckClient: {
      storageKeyGetMetadata: jest.fn().mockResolvedValue({}),
      storageKeyDelete: jest.fn().mockResolvedValue({}),
      projectList: jest.fn().mockResolvedValue([]),
    },
  }),
  url: jest.fn().mockReturnValue({ href: "mockHref" }),
}));
interface KeyStoragePageData {
  modalEdit: boolean;
}
type KeyStoragePageComponent = ComponentPublicInstance & KeyStoragePageData;
let defaultProps: Record<string, any>;

const mountKeyStoragePage = async (props = {}) => {
  return mount(KeyStoragePage as unknown as KeyStoragePageComponent, {
    props: {
      ...defaultProps,
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
  }) as unknown as VueWrapper<KeyStoragePageComponent>;
};
describe("KeyStoragePage.vue", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    defaultProps = {
      project: "test-project",
      readOnly: true,
      allowUpload: true,
      modelValue: "",
      storageFilter: "",
    };
  });
  it("opens the editor when the open-editor event is emitted", async () => {
    const wrapper = await mountKeyStoragePage();
    const keyStorageView = wrapper.findComponent({ name: "KeyStorageView" });
    await wrapper.vm.$nextTick();
    const upload = {
      modifyMode: false,
      keyType: "privateKey",
      inputPath: "/keys/testKey",
      fileName: "testKey",
      file: null,
      fileContent: "",
      textArea: "some-text",
      status: "new",
      password: "",
      errorMsg: null,
      dontOverwrite: false,
    };
    await keyStorageView.vm.$emit("openEditor", upload);
    await wrapper.vm.$nextTick();
    const modal = wrapper.findComponent('[data-testid="modal-edit"]');
    expect(modal.exists()).toBe(true);
    const emittedEvents = keyStorageView.emitted().openEditor;
    expect(emittedEvents[0]).toEqual([upload]);
  });
  it("closes the editor when cancel-editing event is emitted", async () => {
    const wrapper = await mountKeyStoragePage();
    await wrapper.vm.$nextTick();
    const keyStorageEdit = wrapper.findComponent({ name: "KeyStorageEdit" });
    await keyStorageEdit.vm.$emit("cancelEditing");
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.modalEdit).toBe(false);
    const emittedEvents = keyStorageEdit.emitted();
    expect(emittedEvents["cancelEditing"][0]).toEqual([]);
  });
  it("emits finishEditing when a key is created or modified", async () => {
    const wrapper = await mountKeyStoragePage();
    await wrapper.vm.$nextTick();
    const keyStorageEdit = wrapper.findComponent({ name: "KeyStorageEdit" });
    const selectedKey = {
      name: "newKey",
      path: "/keys/newKey",
      keyType: "privateKey",
      modifyMode: true,
      fileContent: "file-content",
      textArea: "some-text",
      status: "update",
      password: "password",
      errorMsg: null,
    };
    await keyStorageEdit.vm.$emit("finishEditing", selectedKey);
    await wrapper.vm.$nextTick();
    const emittedEvents = keyStorageEdit.emitted().finishEditing;
    expect(emittedEvents[0]).toEqual([selectedKey]);
    // Verify that the modal is closed after finishEditing is handled
    expect(wrapper.vm.modalEdit).toBe(false);
  });
  it("updates selectedKey when key-created event is emitted", async () => {
    const wrapper = await mountKeyStoragePage();
    const newKey = {
      name: "testKey",
      path: "/keys/createdKey",
      keyType: "privateKey",
      meta: { rundeckKeyType: "private" },
    };
    const files = [
      {
        name: newKey.name,
        path: newKey.path,
        meta: { rundeckKeyType: "private" },
      },
    ];
    const keyStorageEdit = wrapper.findComponent({ name: "KeyStorageEdit" });
    await keyStorageEdit.vm.$emit("keyCreated", newKey);
    expect(wrapper.emitted().keyCreated);
    await wrapper.vm.$nextTick();
    const keyStorageView = wrapper.findComponent({ name: "KeyStorageView" });
    const addKeyButton = wrapper.find('[data-testid="add-key-btn"]');
    await addKeyButton.trigger("click");
    await wrapper.vm.$nextTick();
    keyStorageView.vm.files = files;
    await wrapper.vm.$nextTick();
    const createdKeyDisplay = wrapper.find('[data-testid="created-key"]');
    expect(createdKeyDisplay.text()).toBe("testKey");
  });
});
