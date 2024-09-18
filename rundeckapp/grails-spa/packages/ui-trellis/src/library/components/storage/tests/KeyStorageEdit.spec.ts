import { mount } from "@vue/test-utils";
import KeyStorageEdit from "../KeyStorageEdit.vue";
import { getRundeckContext } from "../../../rundeckService";
interface StorageKeyMaterialResponse {
  _response: {
    status: number;
  };
}
interface StorageKeyCreateResponse {
  success: boolean;
  keyDetails: {
    name: string;
    path: string;
    keyType: string;
  };
}
jest.mock("../../../rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    rundeckClient: {
      storageKeyGetMaterial: jest.fn().mockResolvedValue({
        _response: { status: 404 },
      } as StorageKeyMaterialResponse),
      storageKeyUpdate: jest.fn().mockResolvedValue({
        success: true,
      }),
      storageKeyCreate: jest.fn().mockResolvedValue({
        success: true,
        keyDetails: {
          name: "exampleKey",
          path: "/keys/test",
          keyType: "privateKey",
        },
      } as StorageKeyCreateResponse),
      storageKeyGetMetadata: jest.fn().mockResolvedValue({
        _response: { status: 200 },
        resources: [],
      }),
    },
  }),
}));

const mountKeyStorageEdit = async (props = {}) => {
  return mount(KeyStorageEdit, {
    props: {
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

      ...props,
    },
  });
};
describe("KeyStorageEdit", () => {
  afterEach(() => {
    jest.clearAllMocks();
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
    const storageKeyCreateMockSpy = getRundeckContext().rundeckClient
      .storageKeyCreate as jest.Mock;
    const saveButton = wrapper.find('[data-testid="save-btn"]');
    await saveButton.trigger("click");
    await wrapper.vm.$nextTick();
    expect(storageKeyCreateMockSpy).toHaveBeenCalledTimes(1);
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
    // Clear the key name input (set it to an empty string)
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
    await wrapper.vm.$nextTick();
    const storageKeyCreateMock = getRundeckContext().rundeckClient
      .storageKeyCreate as jest.Mock;
    expect(storageKeyCreateMock).toHaveBeenCalledWith(
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
    await wrapper.vm.$nextTick();
    const storageKeyCreateMock = getRundeckContext().rundeckClient
      .storageKeyCreate as jest.Mock;
    const saveButton = wrapper.find('[data-testid="save-btn"]');
    await saveButton.trigger("click");
    await wrapper.vm.$nextTick();
    expect(storageKeyCreateMock).toHaveBeenCalledTimes(1);
    expect(wrapper.emitted().finishEditing).toBeTruthy();
  });

  it("emits an error when key exists and overwrite is disabled", async () => {
    const wrapper = await mountKeyStorageEdit({
      uploadSetting: {
        inputType: "text",
        textArea: "some-text",
        dontOverwrite: true,
      },
    });

    const rundeckClientMock = getRundeckContext().rundeckClient;
    (
      rundeckClientMock.storageKeyGetMaterial as jest.Mock
    ).mockResolvedValueOnce({
      _response: { status: 200 }, // key exists
    } as StorageKeyMaterialResponse);
    const saveButton = wrapper.find('[data-testid="save-btn"]');
    await saveButton.trigger("click");
    await wrapper.vm.$nextTick();
    const errorMsg = wrapper.find('[data-testid="error-msg"]');
    expect(errorMsg.text()).toBe("key already exists");
    // storageKeyCreate was not called since overwriting is disabled
    const storageKeyCreateMock =
      rundeckClientMock.storageKeyCreate as jest.Mock;
    expect(storageKeyCreateMock).not.toHaveBeenCalled();
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
    const rundeckClientMock = getRundeckContext().rundeckClient;

    (
      rundeckClientMock.storageKeyGetMaterial as jest.Mock
    ).mockResolvedValueOnce({
      _response: { status: 404 }, // key does not exist
    } as StorageKeyMaterialResponse);

    // Mocking the storageKeyCreate method for when the key is created
    (rundeckClientMock.storageKeyCreate as jest.Mock).mockResolvedValueOnce({
      success: true,
      keyDetails: {
        name: "newKey",
        path: "/keys/newKey",
        keyType: "privateKey",
      },
    } as StorageKeyCreateResponse);

    const saveButton = wrapper.find('[data-testid="save-btn"]');
    await saveButton.trigger("click");
    await wrapper.vm.$nextTick();

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
    expect(rundeckClientMock.storageKeyCreate).toHaveBeenCalledTimes(1);
    expect(wrapper.emitted().finishEditing[0]).toEqual(expectedEmittedEvent);
  });
});
