import { flushPromises, mount } from "@vue/test-utils";
import { Modal } from "uiv";
import { ComponentPublicInstance } from "vue";
import { getRundeckContext } from "../../../rundeckService";
import { listProjects } from "../../../services/projects";
import {
  storageKeyDelete,
  storageKeyGetMetadata,
} from "../../../services/storage";
import KeyStorageView from "../KeyStorageView.vue";

jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    eventBus: { on: jest.fn(), emit: jest.fn() },
    rdBase: "http://localhost:4440/",
    projectName: "testProject",
    apiVersion: "44",
  })),
}));
jest.mock("../../../services/projects");
jest.mock("../../../services/storage");

const mockedListProjects = listProjects as jest.MockedFunction<
  typeof listProjects
>;
mockedListProjects.mockResolvedValue([]);
const mockedStorageKeyGetMetadata =
  storageKeyGetMetadata as jest.MockedFunction<typeof storageKeyGetMetadata>;
mockedStorageKeyGetMetadata.mockResolvedValue({
  resources: [
    {
      name: "/myKey",
      path: "/keys/myKey",
      type: "file",
      meta: { "Rundeck-key-type": "private" },
    },
    {
      name: "/key2",
      path: "/keys/key2",
      type: "file",
      meta: { "Rundeck-key-type": "private" },
    },
  ],
});
const mockedStorageKeyDelete = storageKeyDelete as jest.MockedFunction<
  typeof storageKeyDelete
>;
mockedStorageKeyDelete.mockResolvedValue(true);

// Define the necessary interfaces for the KeyStorageView component
interface KeyStorageViewComponent extends ComponentPublicInstance {
  files: { name: string; path: string; meta: { "Rundeck-key-type": string } }[];
  isSelectedKey: boolean;
  selectedKey: {
    name: string;
    path: string;
    meta: { "Rundeck-key-type": string };
  };
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
      allowDownload: true,
      ...props,
    },
    global: {
      components: {
        Modal,
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
        meta: { "Rundeck-key-type": "private" },
      },
      {
        name: "/key2",
        path: "/keys/key2",
        type: "file",
        meta: { "Rundeck-key-type": "private" },
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
    mockedStorageKeyGetMetadata.mockResolvedValueOnce({
      resources: [
        {
          name: "/key2",
          path: "/keys/key2",
          type: "file",
          meta: { "Rundeck-key-type": "private" },
        },
      ],
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
    expect(mockedStorageKeyDelete).toHaveBeenCalledWith("/myKey");
    expect(vm.files[0].name).toBe("/key2");
    expect(vm.files).toHaveLength(1);
  });
  it.each([
    ["Rundeck-data-type=password", ["/keys/key4"]],
    ["Rundeck-key-type=private", ["/keys/key2", "/keys/key3"]],
    ["Rundeck-key-type=public", ["/keys/key1"]],
  ])(
    "filters key values when storageFilter is %p",
    async (storageFilter: string, expectedKeys: string[]) => {
      mockedStorageKeyGetMetadata.mockResolvedValueOnce({
        resources: [
          {
            name: "/key2",
            path: "/keys/key2",
            type: "file",
            meta: { "Rundeck-key-type": "private" },
          },
          {
            name: "/key1",
            path: "/keys/key1",
            type: "file",
            meta: { "Rundeck-key-type": "public" },
          },
          {
            name: "/key3",
            path: "/keys/key3",
            type: "file",
            meta: { "Rundeck-key-type": "private" },
          },
          {
            name: "/key4",
            path: "/keys/key4",
            type: "file",
            meta: { "Rundeck-data-type": "password" },
          },
        ],
      });
      const wrapper = await mountKeyStorageView({ storageFilter });
      const vm = wrapper.vm as unknown as KeyStorageViewComponent;
      await wrapper.vm.$nextTick();

      // Ensure we have 2 files before deletion
      expect(vm.files).toHaveLength(expectedKeys.length);
      expect(vm.files.map((f) => f.path)).toStrictEqual(expectedKeys);
    },
  );
});
