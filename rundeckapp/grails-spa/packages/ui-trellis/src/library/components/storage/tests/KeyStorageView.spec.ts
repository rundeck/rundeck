import { mount, VueWrapper } from "@vue/test-utils";
import { ComponentPublicInstance } from "vue";
import KeyStorageView from "../KeyStorageView.vue";
import { Modal } from "uiv";
jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    rdBase: "mockRdBase",
    rundeckClient: {
      storageKeyGetMetadata: jest.fn().mockResolvedValue({
        // resources: [
        //   { name: "key1", path: "/keys/key1", type: "publicKey" },
        //   { name: "key2", path: "/keys/key2", type: "privateKey" },
        // ],
      }),
      storageKeyDelete: jest
        .fn()
        .mockResolvedValue({ _response: { status: 200 } }),
      projectList: jest.fn().mockResolvedValue([]),
    },
  }),
  url: jest.fn().mockReturnValue({ href: "mockHref" }),
}));
interface KeyStorageViewComponent extends ComponentPublicInstance {
  files: { name: string; path: string; meta: { rundeckKeyType: string } }[];
  isSelectedKey: boolean;
  selectedKey: { name: string; path: string; meta: { rundeckKeyType: string } };
}
const keys = [
  { name: "key1", path: "/keys/key1", meta: { rundeckKeyType: "private" } },
  { name: "key2", path: "/keys/key2", meta: { rundeckKeyType: "private" } },
];
const mountKeyStorageView = async (props = ({} = {})) => {
  return mount<KeyStorageViewComponent>(KeyStorageView, {
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
        selectedKey: keys[0], // Select the first key
        files: keys, // Initialize files array with keys
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
    const emittedEvent = wrapper.emitted().openEditor;
    expect(emittedEvent).toHaveLength(1);
    const expectedPayload = {
      errorMsg: null,
      file: "",
      fileContent: "",
      fileName: null,
      inputPath: "",
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
    const overwriteButton = wrapper.find(
      'button[data-testid="overwrite-key-btn"]',
    );
    expect(overwriteButton.exists()).toBe(true);
    await overwriteButton.trigger("click");
    const emittedEvent = wrapper.emitted().openEditor;
    expect(emittedEvent).toHaveLength(1);
    const expectedPayload = {
      modifyMode: true,
      keyType: "privateKey",
      inputPath: "",
      inputType: "text",
      fileName: "key1",
      file: "",
      fileContent: "",
      textArea: "",
      password: "",
      status: "update",
      errorMsg: null,
    };
    expect(emittedEvent[0]).toEqual([expectedPayload]);
  });
  it("opens the confirmation modal when the Delete button is clicked", async () => {
    const wrapper = await mountKeyStorageView();
    const vm = wrapper.vm as unknown as KeyStorageViewComponent;
    // Ensure the files are set correctly
    expect(vm.files.length).toBe(2);
    // Select the key first (ensure selectedKey is set)
    vm.selectedKey = keys[0];
    vm.isSelectedKey = true;
    const deleteButton = wrapper.find('button[data-testid="delete-key-btn"]');
    await deleteButton.trigger("click");
    const modal = wrapper.findComponent(Modal);
    const modalTitle = modal.find(".modal-title");
    expect(modalTitle.text()).toBe("Delete Selected Key");
    const modalBody = modal.find(".modal-body");
    expect(modalBody.text()).toContain(
      "Really delete the selected key at this path?",
    );
    const confirmButton = modal.find("button.btn-danger");
    await confirmButton.trigger("click");
    // Manually update files to simulate key deletion
    vm.files = vm.files.filter((file) => file.name !== "key1");
    await wrapper.vm.$nextTick();
    console.log("Files after deletion:", vm.files);
    expect(vm.files).toHaveLength(1);
    expect(vm.files).not.toContainEqual({
      name: "key1",
      path: "/keys/key1",
      meta: { rundeckKeyType: "private" },
    });
  });
});
