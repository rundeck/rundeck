import { mount } from "@vue/test-utils";
import KeyStorageView from "../KeyStorageView.vue";
import { Modal } from "uiv";

jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    rdBase: "mockRdBase",
    rundeckClient: {
      storageKeyGetMetadata: jest.fn().mockResolvedValue({}),
      projectList: jest.fn().mockResolvedValue([]),
    },
  }),
  url: jest.fn().mockReturnValue({ href: "mockHref" }),
}));
const mountKeyStorageView = async (props = ({} = {})) => {
  return mount(KeyStorageView, {
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
        selectedKey: {
          name: "key1",
          path: "/keys/key1",
          meta: {
            rundeckKeyType: "private",
          },
        },
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
    const deleteButton = wrapper.find('button[data-testid="delete-key-btn"]');
    await deleteButton.trigger("click");
    const modal = wrapper.findComponent(Modal);
    const modalTitle = modal.find(".modal-title");
    expect(modalTitle.text()).toBe("Delete Selected Key");
    const modalBody = modal.find(".modal-body");
    expect(modalBody.text()).toContain(
      "Really delete the selected key at this path?",
    );
  });
});
