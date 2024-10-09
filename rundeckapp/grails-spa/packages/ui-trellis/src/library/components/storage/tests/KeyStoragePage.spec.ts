import { flushPromises, mount, VueWrapper } from "@vue/test-utils";
import { ComponentPublicInstance } from "vue";
import KeyStoragePage from "../KeyStoragePage.vue";
import { getRundeckContext } from "../../../rundeckService";
import { Modal } from "uiv";
jest.mock("../../../rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    rdBase: "mockRdBase",
    projectName: "test-project",
    rundeckClient: {
      storageKeyGetMetadata: jest.fn().mockResolvedValue({
        resources: [
          {
            name: "newKey",
            path: "/keys/newKey",
            type: "file",
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
  let rundeckClientMock: any;
  beforeEach(() => {
    rundeckClientMock = getRundeckContext().rundeckClient;
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
    await wrapper.vm.$nextTick();
    // 1. First mock: Initial files before any event
    rundeckClientMock.storageKeyGetMetadata.mockResolvedValueOnce({
      resources: [
        {
          name: "testKey",
          path: "/keys/testKey",
          type: "file",
          meta: { rundeckKeyType: "private" },
        },
      ],
      _response: { status: 200 },
    });
    const keyStorageView = wrapper.findComponent({ name: "KeyStorageView" });
    const newKey = {
      name: "newKey",
      path: "/keys/newKey",
      keyType: "privateKey",
      meta: { rundeckKeyType: "private" },
    };
    await flushPromises();
    await wrapper.vm.$nextTick();
    // 2. Second mock: After the  event, reflecting the new key
    rundeckClientMock.storageKeyGetMetadata.mockResolvedValueOnce({
      resources: [
        {
          name: "newKey",
          path: "/keys/newKey",
          type: "file",
          meta: { rundeckKeyType: "private" },
        },
      ],
      _response: { status: 200 },
    });
    const keyStorageEdit = wrapper.findComponent({ name: "KeyStorageEdit" });
    await keyStorageEdit.vm.$emit("keyCreated", newKey);

    const addKeyButton = wrapper.find('[data-testid="add-key-btn"]');
    await addKeyButton.trigger("click");
    await flushPromises();
    await wrapper.vm.$nextTick();
    // Ensure that the files array is updated after the event and button click
    expect(keyStorageView.vm.files.length).toBe(1);
    expect(keyStorageView.vm.files).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          name: "newKey",
          path: "/keys/newKey",
        }),
      ]),
    );
  });
});
