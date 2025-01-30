import {flushPromises, mount} from '@vue/test-utils'
import {storageKeyCreate, storageKeyExists, storageKeyGetMetadata} from '../../../services/storage'
import KeyStorageEdit from '../KeyStorageEdit.vue'

jest.mock('@/library/rundeckService', () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    eventBus: {on: jest.fn(), emit: jest.fn()},
    rdBase: 'http://localhost:4440/',
    projectName: 'testProject',
    apiVersion: '44',
  })),
}));
jest.mock('../../../services/storage')

const mockedStorageKeyGetMetadata = storageKeyGetMetadata as jest.MockedFunction<
  typeof storageKeyGetMetadata
>
mockedStorageKeyGetMetadata.mockResolvedValue({resources: []})
const mockedStorageKeyExists = storageKeyExists as jest.MockedFunction<
  typeof storageKeyExists
>
mockedStorageKeyExists.mockResolvedValue(false)
const mockedStorageKeyCreate = storageKeyCreate as jest.MockedFunction<
  typeof storageKeyCreate
>

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

const defaultProps: DefaultProps = {
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

    mockedStorageKeyCreate.mockResolvedValue({
        name: "exampleKey",
        path: "/keys/test",
      type: 'privateKey',
    });
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
    const saveButton = wrapper.find('[data-testid="save-btn"]');
    await saveButton.trigger("click");
    await flushPromises();
    expect(
      mockedStorageKeyCreate,
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
      mockedStorageKeyCreate,
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
      mockedStorageKeyCreate,
    ).toHaveBeenCalledTimes(1);
  });

  it("emits an error when key exists and overwrite is disabled", async () => {
    mockedStorageKeyExists.mockResolvedValueOnce(true/* key exists */)
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
      mockedStorageKeyCreate,
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

    // Mock the storageKeyCreate method to return success
    mockedStorageKeyCreate.mockResolvedValueOnce({
        name: "newKey",
        path: "/keys/newKey",
        type: 'privateKey',
    });

    const saveButton = wrapper.find('[data-testid="save-btn"]');
    await saveButton.trigger("click");
    await flushPromises();

    const expectedEmittedEvent = [
      {
          name: "newKey",
          path: "/keys/newKey",
        type: 'privateKey',
      },
    ];

    // Verify that the storageKeyCreate method is called
    expect(
      mockedStorageKeyCreate
    ).toHaveBeenCalledTimes(1);

    // Verify that the finishEditing event is emitted with the correct data
    expect(wrapper.emitted().finishEditing[0]).toEqual(expectedEmittedEvent);
  });
});
