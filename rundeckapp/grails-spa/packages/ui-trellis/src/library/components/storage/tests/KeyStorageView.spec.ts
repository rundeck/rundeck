import { flushPromises, mount } from "@vue/test-utils";
import { ComponentPublicInstance } from "vue";
import KeyStorageView from "../KeyStorageView.vue";
import { getRundeckContext } from "../../../rundeckService";
import { Modal } from "uiv";
jest.mock("../../../rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    rdBase: "mockRdBase",
    rundeckClient: {
      storageKeyGetMetadata: jest.fn().mockResolvedValue({
        resources: [
          {
            name: "/myKey",
            path: "/keys/myKey",
            type: "file",
            meta: { rundeckKeyType: "private" },
          },
          {
            name: "/key2",
            path: "/keys/key2",
            type: "file",
            meta: { rundeckKeyType: "private" },
          },
        ],
        _response: { status: 200 },
      }),
      storageKeyDelete: jest.fn().mockResolvedValue({
        _response: { status: 200 },
      }),
      projectList: jest.fn().mockResolvedValue([]),
    },
  }),
  url: jest.fn().mockReturnValue({ href: "mockHref" }),
}));

// Define the necessary interfaces for the KeyStorageView component
interface KeyStorageViewComponent extends ComponentPublicInstance {
  files: { name: string; path: string; meta: { rundeckKeyType: string } }[];
  isSelectedKey: boolean;
  selectedKey: { name: string; path: string; meta: { rundeckKeyType: string } };
  confirmDeleteKey: () => Promise<void>;
  loadKeys: () => Promise<void>;
}

let rundeckClientMock: any;
let keys: any[];
const mountKeyStorageView = async (props = {}) => {
  return mount<KeyStorageViewComponent>(KeyStorageView, {
    props: {
      rootPath: "",
      readOnly: false,
      allowUpload: true,
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
    data() {
      return {
        files: keys, // Set the files to the `keys` array
        isSelectedKey: true,
        selectedKey: keys[0], // Pre-select the first key
      };
    },
  });
};

describe("KeyStorageView", () => {
  beforeEach(() => {
    keys = [
      {
        name: "/myKey",
        path: "/keys/myKey",
        type: "file",
        meta: { rundeckKeyType: "private" },
      },
      {
        name: "/key2",
        path: "/keys/key2",
        type: "file",
        meta: { rundeckKeyType: "private" },
      },
    ];
    rundeckClientMock = getRundeckContext().rundeckClient;
    jest.clearAllMocks();
  });

  it("emits openEditor when the Add or Upload a Key button is clicked", async () => {
    const wrapper = await mountKeyStorageView();
    const input = wrapper.find('input[type="text"]');
    await input.setValue("newKey");
    const addButton = wrapper.find('button[data-testid="add-key-btn"]');
    await addButton.trigger("click");
    const emittedEvent = wrapper.emitted().openEditor;
    expect(emittedEvent).toHaveLength(1);
    const expectedPayload = {
      errorMsg: null,
      file: "",
      fileContent: "",
      fileName: null,
      inputPath: "newKey",
      inputType: "text",
      keyType: "privateKey",
      modifyMode: false,
      password: "",
      status: "new",
      textArea: "",
    };
    expect(emittedEvent[0]).toEqual([expectedPayload]);
  });
  it("emits openEditor when the Overwrite Key button is clicked", async () => {
    const wrapper = await mountKeyStorageView();
    await wrapper.vm.$nextTick();
    const keyItem = wrapper.find('[data-testid="created-key"]');
    await keyItem.trigger("click");
    await wrapper.vm.$nextTick();
    const overwriteButton = wrapper.find(
      'button[data-testid="overwrite-key-btn"]',
    );
    await overwriteButton.trigger("click");
    await wrapper.vm.$nextTick();
    const emittedEvent = wrapper.emitted().openEditor;
    expect(emittedEvent).toHaveLength(1);
    const expectedPayload = {
      modifyMode: true,
      keyType: "privateKey",
      inputPath: "keys",
      inputType: "text",
      fileName: "/key2",
      file: "",
      fileContent: "",
      textArea: "",
      password: "",
      status: "update",
      errorMsg: null,
    };
    expect(emittedEvent[0]).toEqual([expectedPayload]);
  });
  it("opens the confirmation modal and deletes the selected key", async () => {
    const wrapper = await mountKeyStorageView();
    const vm = wrapper.vm as unknown as KeyStorageViewComponent;
    await wrapper.vm.$nextTick();
    rundeckClientMock.storageKeyGetMetadata.mockResolvedValueOnce({
      resources: [
        {
          name: "/key2",
          path: "/keys/key2",
          type: "file",
          meta: { rundeckKeyType: "private" },
        },
      ],
      _response: { status: 200 },
    });

    // Ensure we have 2 files before deletion
    expect(vm.files).toHaveLength(2);

    // Simulate the deletion button click
    const deleteButton = wrapper.find('button[data-testid="delete-key-btn"]');
    await deleteButton.trigger("click");
    await wrapper.vm.$nextTick();

    // Assert that the modal opens
    const modal = wrapper.findComponent(Modal);
    const modalTitle = modal.find(".modal-title");
    expect(modalTitle.text()).toBe("Delete Selected Key");

    // Simulate confirmation of deletion
    const confirmButton = modal.find(
      'button[data-testid="confirm-delete-btn"]',
    );
    expect(confirmButton.exists()).toBe(true);
    await confirmButton.trigger("click");
    await flushPromises();
    expect(rundeckClientMock.storageKeyDelete).toHaveBeenCalledWith("/myKey");
    expect(vm.files[0].name).toBe("/key2");
    expect(vm.files).toHaveLength(1);
  });
});
