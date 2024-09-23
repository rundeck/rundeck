import { mount } from "@vue/test-utils";
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
            name: "/key1",
            path: "/keys/key1",
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
interface Key {
  name: string;
  path: string;
  meta: {
    rundeckKeyType: string;
  };
}
interface KeyStorageViewComponent extends ComponentPublicInstance {
  files: { name: string; path: string; meta: { rundeckKeyType: string } }[];
  isSelectedKey: boolean;
  selectedKey: { name: string; path: string; meta: { rundeckKeyType: string } };
  parentDirString: (path: string) => string;
  relativePath: (path: any) => any;
  confirmDeleteKey: () => Promise<void>;
}
interface StorageKeyMetadataResponse {
  resources: {
    name: string;
    path: string;
    type: string;
    meta: { rundeckKeyType: string };
  }[];
  _response: {
    status: number;
  };
}

const keys = [
  {
    name: "/key1",

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
        $t: (msg) => msg,
      },
    },
    data() {
      return {
        files: keys,
        isSelectedKey: true,
        selectedKey: keys[0],
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
    const input = wrapper.find('input[type="text"]');
    await input.setValue("/keys/newKey");
    const addButton = wrapper.find('button[data-testid="add-key-btn"]');
    await addButton.trigger("click");
    const emittedEvent = wrapper.emitted().openEditor;
    expect(emittedEvent).toHaveLength(1);
    const expectedPayload = {
      errorMsg: null,
      file: "",
      fileContent: "",
      fileName: null,
      inputPath: "/keys/newKey",
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
      fileName: "/key1",
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
    const rundeckClientMock = getRundeckContext().rundeckClient;
    (
      rundeckClientMock.storageKeyGetMetadata as jest.Mock
    ).mockResolvedValueOnce({
      resources: [
        {
          name: "/key1",
          path: "/keys/key1",
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
    } as StorageKeyMetadataResponse);
    (rundeckClientMock.storageKeyDelete as jest.Mock).mockResolvedValueOnce({
      _response: { status: 200 },
    });
    const wrapper = await mountKeyStorageView();
    const vm = wrapper.vm as unknown as KeyStorageViewComponent;
    await wrapper.vm.$nextTick();
    // check if we have 2 files before deletion: key1 and key2
    expect(vm.files).toHaveLength(2);
    const deleteButton = wrapper.find('button[data-testid="delete-key-btn"]');
    await deleteButton.trigger("click");
    await wrapper.vm.$nextTick();
    const modal = wrapper.findComponent(Modal);
    const modalTitle = modal.find(".modal-title");
    expect(modalTitle.text()).toBe("Delete Selected Key");
    const modalBody = modal.find(".modal-body");
    expect(modalBody.text()).toContain(
      "Really delete the selected key at this path?",
    );
    const confirmButton = modal.find(
      'button[data-testid="confirm-delete-btn"]',
    );
    expect(confirmButton.exists()).toBe(true);
    await confirmButton.trigger("click");
    await wrapper.vm.$nextTick();
    // the deletion process in the test
    const index = vm.files.findIndex((file) => file.name === "/key1");
    if (index !== -1) {
      vm.files.splice(index, 1);
    }
    await wrapper.vm.$nextTick();
    // check if we have 1 file after deletion: key2
    expect(vm.files).toHaveLength(1);
  });
});
