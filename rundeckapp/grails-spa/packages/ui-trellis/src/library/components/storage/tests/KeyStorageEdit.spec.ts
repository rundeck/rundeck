import { flushPromises, mount } from "@vue/test-utils";
import KeyStorageEdit from "../KeyStorageEdit.vue";
import { getRundeckContext } from "../../../rundeckService";

jest.mock("../../../rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    rundeckClient: {
      storageKeyGetMaterial: jest.fn().mockResolvedValue({
        _response: { status: 404 },
      }),
      storageKeyUpdate: jest.fn().mockResolvedValue({
        success: true,
      }),
      storageKeyCreate: jest.fn(),
      storageKeyGetMetadata: jest.fn().mockResolvedValue({
        _response: { status: 200 },
        resources: [],
      }),
    },
  }),
}));

type RundeckClient = ReturnType<typeof getRundeckContext>["rundeckClient"];
type DefaultProps = {
  storageFilter: string;
  uploadSetting: {
    modifyMode: boolean;
    keyType: string;
    inputPath: string;
    fileName: string | null;
    file: File | null;
    fileContent: string;
    textArea: string;
    status: string;
    password: string;
    errorMsg: string | null;
    dontOverwrite: boolean;
  };
  project: string;
};

let rundeckClientMock: RundeckClient;
let defaultProps: DefaultProps;

const mountKeyStorageEdit = async (props = {}) => {
  return mount(KeyStorageEdit, {
    props: {
      ...defaultProps,
      ...props,
    },
  });
};

describe("KeyStorageEdit", () => {
  beforeEach(() => {
    rundeckClientMock = getRundeckContext().rundeckClient;
    jest.clearAllMocks();
    (rundeckClientMock.storageKeyCreate as jest.Mock).mockResolvedValue({
      success: true,
      keyDetails: {
        name: "exampleKey",
        path: "/keys/test",
        keyType: "privateKey",
      },
    });

    defaultProps = {
      storageFilter: "",
      uploadSetting: {
        modifyMode: false,
        keyType: "privateKey",
        inputPath: "",
        fileName: null,
        file: null,
        fileContent: "",
        textArea: "some-text",
        status: "new",
        password: "",
        errorMsg: null,
        dontOverwrite: false,
      },
      project: "test-project",
    };
  });

  it("emits cancelEditing when cancel button is clicked", async () => {
    const wrapper = await mountKeyStorageEdit();
    const cancelButton = wrapper.find('[data-testid="cancel-btn"]');
    await cancelButton.trigger("click");
    expect(wrapper.emitted("cancelEditing")).toHaveLength(1);
  });

  it("emits finishEditing with correct data when Save button is clicked", async () => {
    const wrapper = await mountKeyStorageEdit({
      uploadSetting: {
        keyType: "privateKey",
        inputType: "text",
        textArea: "some-text",
      },
    });
    const saveButton = wrapper.find('[data-testid="save-btn"]');
    await saveButton.trigger("click");
    await flushPromises();
    expect(
      rundeckClientMock.storageKeyCreate as jest.Mock,
    ).toHaveBeenCalledTimes(1);
    expect(wrapper.emitted().finishEditing).toHaveLength(1);
  });

  it("enables and disables the Save button based on key name input", async () => {
    const wrapper = await mountKeyStorageEdit({
      uploadSetting: { textArea: "" },
    });
    const keyNameInput = wrapper.find('[data-testid="key-name-input"]');
    const saveButton = wrapper.find('[data-testid="save-btn"]');
    expect(saveButton.attributes("disabled")).toBe("");
    await keyNameInput.setValue("test-key");
    await wrapper.vm.$nextTick();
    expect(saveButton.attributes("disabled")).toBeFalsy();
    await keyNameInput.setValue("");
    await wrapper.vm.$nextTick();
    expect(saveButton.attributes().disabled).toBeDefined();
  });

  it("handles saving a password key type", async () => {
    const wrapper = await mountKeyStorageEdit({
      uploadSetting: {
        keyType: "password",
        password: "my-password",
        inputType: "text",
        fileName: "exampleKey",
      },
    });
    const saveButton = wrapper.find('[data-testid="save-btn"]');
    await saveButton.trigger("click");
    await flushPromises();
    expect(
      rundeckClientMock.storageKeyCreate as jest.Mock,
    ).toHaveBeenCalledWith(
      expect.any(String),
      "my-password",
      expect.any(Object),
    );
    expect(wrapper.emitted().finishEditing).toHaveLength(1);
  });

  it("handles saving a public key type", async () => {
    const wrapper = await mountKeyStorageEdit({
      uploadSetting: {
        keyType: "publicKey",
        textArea: "public-key-text",
        inputType: "text",
        fileName: "testKey",
        fileContent: "",
      },
    });
    const saveButton = wrapper.find('[data-testid="save-btn"]');
    await saveButton.trigger("click");
    await flushPromises();
    expect(
      rundeckClientMock.storageKeyCreate as jest.Mock,
    ).toHaveBeenCalledTimes(1);
  });

  it("emits an error when key exists and overwrite is disabled", async () => {
    (
      rundeckClientMock.storageKeyGetMaterial as jest.Mock
    ).mockResolvedValueOnce({
      _response: { status: 200 }, // key exists
    });
    const wrapper = await mountKeyStorageEdit({
      uploadSetting: {
        dontOverwrite: true,
        inputType: "text",
        textArea: "some-text",
      },
    });
    const saveButton = wrapper.find('[data-testid="save-btn"]');
    await saveButton.trigger("click");
    await flushPromises();
    const errorMsg = wrapper.find('[data-testid="error-msg"]');
    expect(errorMsg.text()).toBe("key already exists");
    expect(
      rundeckClientMock.storageKeyCreate as jest.Mock,
    ).not.toHaveBeenCalled();
  });

  it("creates a new private key when it does not exist", async () => {
    const wrapper = await mountKeyStorageEdit({
      uploadSetting: {
        keyType: "privateKey",
        inputType: "text",
        fileName: "newKey",
        textArea: "private-key-text",
      },
    });
    // Key doesn't exists
    (
      rundeckClientMock.storageKeyGetMaterial as jest.Mock
    ).mockResolvedValueOnce({
      _response: { status: 404 },
    });
    // creating key
    (rundeckClientMock.storageKeyCreate as jest.Mock).mockResolvedValueOnce({
      success: true,
      keyDetails: {
        name: "newKey",
        path: "/keys/newKey",
        keyType: "privateKey",
      },
    });

    const saveButton = wrapper.find('[data-testid="save-btn"]');
    await saveButton.trigger("click");
    await flushPromises();
    const expectedEmittedEvent = [
      {
        success: true,
        keyDetails: {
          name: "newKey",
          path: "/keys/newKey",
          keyType: "privateKey",
        },
      },
    ];
    expect(
      rundeckClientMock.storageKeyCreate as jest.Mock,
    ).toHaveBeenCalledTimes(1);
    expect(wrapper.emitted().finishEditing[0]).toEqual(expectedEmittedEvent);
  });
});
