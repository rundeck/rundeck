import { mount } from "@vue/test-utils";
import KeyStoragePage from "../KeyStoragePage.vue";
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
    const keyStorageView = wrapper.findComponent({ name: "KeyStorageView" });
    expect(keyStorageView.exists()).toBe(true);
    await keyStorageView.vm.$emit("update:modelValue", "openEditor");
    await wrapper.vm.$nextTick();
    const modal = wrapper.findComponent({ ref: "modalEdit" });
    expect(modal.exists()).toBe(true);
  });

  it("closes the editor when cancel-editing event is emitted from KeyStorageEdit", async () => {
    const wrapper = await mountKeyStoragePage();
    const keyStorageView = wrapper.findComponent({ name: "KeyStorageView" });
    expect(keyStorageView.exists()).toBe(true);
    await keyStorageView.vm.$emit("update:modelValue", "openEditor");
    await wrapper.vm.$nextTick(); // Wait for modal to open
    const keyStorageEdit = wrapper.findComponent({ name: "KeyStorageEdit" });
    expect(keyStorageEdit.exists()).toBe(true);
    await keyStorageEdit.vm.$emit("cancel-editing");
    await wrapper.vm.$nextTick(); // Wait for modal state update
    // Check that the event was emitted
    const emittedEvents = keyStorageEdit.emitted();
    expect(emittedEvents["cancel-editing"]).toBeTruthy();
    expect(wrapper.find("modal").exists()).toBe(false);
  });
  //
  it("emits finishEditing when a key is created or modified", async () => {
    const wrapper = await mountKeyStoragePage();
    const keyStorageView = wrapper.findComponent({ name: "KeyStorageView" });
    expect(keyStorageView.exists()).toBe(true); // Ensure KeyStorageView is rendered
    await keyStorageView.vm.$emit("update:modelValue", "openEditor");
    await wrapper.vm.$nextTick(); // Wait for modal to open
    const keyStorageEdit = wrapper.findComponent({ name: "KeyStorageEdit" });
    expect(keyStorageEdit.exists()).toBe(true);
    // const selectedKey = { name: "newKey", path: "/keys/newKey" };
    await keyStorageEdit.vm.$emit("finishEditing", "result");
    const keyStorageViewInstance = wrapper.findComponent({
      name: "KeyStorageView",
    }).vm;
    const loadKeysSpy = jest.spyOn(keyStorageViewInstance, "loadKeys");
    await wrapper.vm.$nextTick(); // Wait for modal state update
    // expect(loadKeysSpy).toHaveBeenCalledWith(selectedKey);
    const vm = wrapper.vm as unknown as KeyStoragePageData;
    // console.log("Wrapper HTML after finish-editing:", wrapper.html());
    console.log("Modal state after finish-editing:", vm.modalEdit);
    expect(vm.modalEdit).toBe(false);
    // expect(wrapper.findComponent({ ref: "modalEdit" }).exists()).toBe(false);
  });
  //
  it("updates selectedKey when key-created event is emitted", async () => {
    const wrapper = await mountKeyStoragePage();
    const keyStorageEdit = wrapper.findComponent({ name: "KeyStorageEdit" });
    expect(keyStorageEdit.exists()).toBe(true);
    // const newKey = { name: "createdKey", path: "/keys/createdKey" };
    await keyStorageEdit.vm.$emit("keyCreated", {
      name: "createdKey",
      path: "/keys/createdKey",
      keyType: "privateKey",
      meta: {
        rundeckKeyType: "private",
      },
    });
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    // console.log("Wrapper HTML after key-created:", wrapper.html());
    const keyStorageView = wrapper.findComponent({ name: "KeyStorageView" });
    expect(keyStorageView.exists()).toBe(true);
    console.log("KeyStorageView props:", keyStorageView.props());
    await wrapper.vm.$nextTick();
    const createdKeyDisplay = wrapper.find('[data-testid="created-key"]');
    console.log("Created key display:", createdKeyDisplay.exists());
    expect(createdKeyDisplay.exists()).toBe(true);
    // expect(createdKeyDisplay.text()).toBe("createdKey");
  });
});
