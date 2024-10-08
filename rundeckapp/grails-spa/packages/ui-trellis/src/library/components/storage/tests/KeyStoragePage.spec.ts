import { flushPromises, mount, VueWrapper } from "@vue/test-utils";
import { ComponentPublicInstance } from "vue";
import KeyStoragePage from "../KeyStoragePage.vue";
import { getRundeckContext } from "../../../rundeckService";
import { Modal } from "uiv";

jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    rdBase: "mockRdBase",
    projectName: "test-project",
    rundeckClient: {
      storageKeyGetMetadata: jest.fn().mockResolvedValue({
        resources: [
          {
            name: "testKey",
            path: "/keys/testKey",
            meta: { rundeckKeyType: "private" },
          },
        ],
        _response: { status: 200 },
      }),
      storageKeyDelete: jest.fn().mockResolvedValue({}),
      projectList: jest.fn().mockResolvedValue([]),
    },
  }),
  url: jest.fn().mockReturnValue({ href: "mockHref" }),
}));

interface KeyStoragePageData {
  modalEdit: boolean;
  selectedKey: Record<string, any>;
}
type KeyStoragePageComponent = ComponentPublicInstance & KeyStoragePageData;

const mountKeyStoragePage = async (props = {}) => {
  return mount(KeyStoragePage as unknown as KeyStoragePageComponent, {
    props: {
      project: "test-project",
      readOnly: true,
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
        $t: (msg: string) => msg,
      },
    },
  }) as unknown as VueWrapper<KeyStoragePageComponent>;
};
describe("KeyStoragePage.vue", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("opens the editor when the open-editor event is emitted", async () => {
    const wrapper = await mountKeyStoragePage();
    const keyStorageView = wrapper.findComponent({ name: "KeyStorageView" });
    await wrapper.vm.$nextTick();
    const upload = {
      modifyMode: false,
      keyType: "privateKey",
      inputPath: "testKey",
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
    const emittedEvents = keyStorageView.emitted().openEditor;
    expect(emittedEvents[0]).toEqual([upload]);
    expect(wrapper.text()).toContain("Add or Upload a Key");
  });

  it("closes the editor when cancel-editing event is emitted", async () => {
    const wrapper = await mountKeyStoragePage();
    // Simulate the cancelEditing event
    const keyStorageEdit = wrapper.findComponent({ name: "KeyStorageEdit" });
    expect(keyStorageEdit.exists()).toBe(true);
    await keyStorageEdit.vm.$emit("cancelEditing");
    await wrapper.vm.$nextTick();
    await flushPromises();
    const emittedEvents = keyStorageEdit.emitted().cancelEditing;
    expect(emittedEvents).toHaveLength(1);
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
    expect(emittedEvents).toHaveLength(1);
  });
  it("updates selectedKey when key-created event is emitted", async () => {
    const wrapper = await mountKeyStoragePage();
    const newKey = {
      name: "testKey",
      path: "/keys/createdKey",
      keyType: "privateKey",
      meta: { rundeckKeyType: "private" },
    };
    const rundeckClientMock = getRundeckContext().rundeckClient;
    (rundeckClientMock.storageKeyGetMetadata as jest.Mock).mockResolvedValue({
      resources: [
        {
          name: "testKey",
          path: "/keys/createdKey",
          meta: { rundeckKeyType: "private" },
        },
      ],
      _response: { status: 200 },
    });
    const keyStorageEdit = wrapper.findComponent({ name: "KeyStorageEdit" });
    await keyStorageEdit.vm.$emit("keyCreated", newKey);
    await wrapper.vm.$nextTick();
    const addKeyButton = wrapper.find('[data-testid="add-key-btn"]');
    expect(addKeyButton.exists()).toBe(true);
    await addKeyButton.trigger("click");
    await wrapper.vm.$nextTick();
    const keyStorageView = wrapper.findComponent({ name: "KeyStorageView" });
    await flushPromises();

    console.log(
      "Files in KeyStorageView after loadKeys:",
      keyStorageView.vm.files,
    );
    expect(keyStorageView.vm.files.length).toBeGreaterThan(0);
    expect(keyStorageView.vm.files[0].name).toBe("testKey");
    // Check if the created key is displayed in the view
    const createdKeyDisplay = wrapper.find('[data-testid="created-key"]');
    expect(createdKeyDisplay.exists()).toBe(true);
    expect(createdKeyDisplay.text()).toBe("testKey");
  });
});
