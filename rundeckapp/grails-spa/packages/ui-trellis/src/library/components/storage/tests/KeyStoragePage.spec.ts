import { mount } from "@vue/test-utils";
import KeyStoragePage from "../KeyStoragePage.vue";
import KeyStorageView from "../KeyStorageView.vue";
import KeyStorageEdit from "../KeyStorageEdit.vue";
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

interface KeyStoragePageData {
  modalEdit: boolean;
  // Add other properties if needed
}

const mountKeyStoragePage = async (props = {}) => {
  return mount(KeyStoragePage, {
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
  });
};

describe("KeyStoragePage.vue", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it("opens the editor when the open-editor event is emitted from KeyStorageView", async () => {
    const wrapper = await mountKeyStoragePage();
    const keyStorageView = wrapper.findComponent(KeyStorageView);
    expect(keyStorageView.exists()).toBe(true);
    const uploadSetting = { modifyMode: false, keyType: "privateKey" };
    await keyStorageView.vm.$emit("open-editor", uploadSetting);
    await wrapper.vm.$nextTick();

    const modal = wrapper.findComponent({ ref: "modalEdit" });
    console.log("Wrapper HTML after open-editor:", wrapper.html());
    expect(modal.exists()).toBe(true);
  });

  it("closes the editor when cancel-editing event is emitted from KeyStorageEdit", async () => {
    const wrapper = await mountKeyStoragePage();
    const keyStorageView = wrapper.findComponent(KeyStorageView);
    expect(keyStorageView.exists()).toBe(true);
    // Emit the open-editor event to open the modal
    const uploadSetting = { modifyMode: false, keyType: "privateKey" };
    await keyStorageView.vm.$emit("open-editor", uploadSetting);
    await wrapper.vm.$nextTick(); // Wait for modal to open
    // Emit the cancel-editing event to close the modal
    const keyStorageEdit = wrapper.findComponent(KeyStorageEdit);
    expect(keyStorageEdit.exists()).toBe(true);

    await keyStorageEdit.vm.$emit("cancel-editing");
    await wrapper.vm.$nextTick(); // Wait for modal state to update
    const vm = wrapper.vm as unknown as KeyStoragePageData;
    console.log("Wrapper HTML after cancel-editing:", wrapper.html());
    console.log("Modal state after cancel-editing:", vm.modalEdit);
    expect(vm.modalEdit).toBe(false);
    expect(wrapper.findComponent({ ref: "modalEdit" }).exists()).toBe(false);
  });

  it("emits finishEditing when a key is created or modified", async () => {
    const wrapper = await mountKeyStoragePage();
    const keyStorageView = wrapper.findComponent(KeyStorageView);
    expect(keyStorageView.exists()).toBe(true); // Ensure KeyStorageView is rendered

    // Emit the open-editor event to open the modal
    const uploadSetting = { modifyMode: false, keyType: "privateKey" };
    await keyStorageView.vm.$emit("open-editor", uploadSetting);
    await wrapper.vm.$nextTick(); // Wait for modal to open

    // Emit the finish-editing event from KeyStorageEdit
    const keyStorageEdit = wrapper.findComponent(KeyStorageEdit);
    expect(keyStorageEdit.exists()).toBe(true);
    const selectedKey = { name: "newKey", path: "/keys/newKey" };
    await keyStorageEdit.vm.$emit("finish-editing", selectedKey);

    // Spy on the loadKeys method
    const keyStorageViewInstance = wrapper.findComponent(KeyStorageView).vm;
    const loadKeysSpy = jest.spyOn(keyStorageViewInstance, "loadKeys");
    await wrapper.vm.$nextTick(); // Wait for modal state update
    expect(loadKeysSpy).toHaveBeenCalledWith(selectedKey);
    const vm = wrapper.vm as unknown as KeyStoragePageData;
    console.log("Wrapper HTML after finish-editing:", wrapper.html());
    console.log("Modal state after finish-editing:", vm.modalEdit);
    expect(vm.modalEdit).toBe(false);
    expect(wrapper.findComponent({ ref: "modalEdit" }).exists()).toBe(false);
  });

  it("updates selectedKey when key-created event is emitted from KeyStorageEdit", async () => {
    const wrapper = await mountKeyStoragePage();
    const keyStorageEdit = wrapper.findComponent(KeyStorageEdit);
    expect(keyStorageEdit.exists()).toBe(true);
    const newKey = { name: "createdKey", path: "/keys/createdKey" };
    await keyStorageEdit.vm.$emit("key-created", newKey);
    await wrapper.vm.$nextTick(); // Wait for state update
    const keyStorageView = wrapper.findComponent(KeyStorageView);
    expect(keyStorageView.exists()).toBe(true);
    expect(keyStorageView.props("createdKey")).toEqual(newKey);
  });
});
