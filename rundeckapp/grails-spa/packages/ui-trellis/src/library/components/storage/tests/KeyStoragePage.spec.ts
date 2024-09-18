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
const mountKeyStoragePage = async (
  props = {},
): Promise<VueWrapper<KeyStoragePageComponent>> => {
  return mount(KeyStoragePage as unknown as KeyStoragePageComponent, {
    props: {
      project: "test-project",
      readOnly: false,
      allowUpload: true,
      modelValue: "",
      storageFilter: "",
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
  afterEach(() => {
    jest.clearAllMocks();
  });

  it("opens the editor when the open-editor event is emitted", async () => {
    const wrapper = await mountKeyStoragePage();
    const keyStorageView = wrapper.findComponent({ name: "KeyStorageView" });
    expect(keyStorageView.exists()).toBe(true);
    await keyStorageView.vm.$emit("openEditor");
    await wrapper.vm.$nextTick();
    const modal = wrapper.findComponent('[data-testid="modal-edit"]');
    expect(modal.exists()).toBe(true);
    expect(modal.isVisible()).toBe(true);
  });

  it("closes the editor when cancel-editing event is emitted", async () => {
    const wrapper = await mountKeyStoragePage();
    const keyStorageView = wrapper.findComponent({ name: "KeyStorageView" });
    expect(keyStorageView.exists()).toBe(true);
    await keyStorageView.vm.$emit("openEditor");
    await wrapper.vm.$nextTick();
    const keyStorageEdit = wrapper.findComponent({ name: "KeyStorageEdit" });
    await keyStorageEdit.vm.$emit("cancelEditing");
    await wrapper.vm.$nextTick();
    const emittedEvents = keyStorageEdit.emitted();
    expect(emittedEvents["cancelEditing"][0]).toEqual([]);
  });

  it("emits finishEditing when a key is created or modified", async () => {
    const wrapper = await mountKeyStoragePage();
    const keyStorageView = wrapper.findComponent({ name: "KeyStorageView" });
    await keyStorageView.vm.$emit("update:modelValue", "openEditor");
    await wrapper.vm.$nextTick();
    const keyStorageEdit = wrapper.findComponent({ name: "KeyStorageEdit" });
    expect(keyStorageEdit.exists()).toBe(true);
    const selectedKey = { name: "newKey", path: "/keys/newKey" };
    await keyStorageEdit.vm.$emit("finishEditing", selectedKey);
    await wrapper.vm.$nextTick();
    const keyStorageViewInstance = wrapper.findComponent({
      name: "KeyStorageView",
    }).vm;
    const loadKeysSpy = jest.spyOn(keyStorageViewInstance, "loadKeys");
    await keyStorageViewInstance.loadKeys(selectedKey);
    expect(loadKeysSpy).toHaveBeenCalledWith(selectedKey);
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
