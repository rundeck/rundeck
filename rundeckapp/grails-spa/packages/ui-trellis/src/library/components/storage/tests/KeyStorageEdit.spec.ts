import { mount } from "@vue/test-utils";
import KeyStorageEdit from "../KeyStorageEdit.vue";

const mountKeyStorageEdit = async (props = {}) => {
  return mount(KeyStorageEdit, {
    props: {
      storageFilter: "",
      uploadSetting: {
        modifyMode: false,
        keyType: "privateKey",
        inputPath: "",
        inputType: "text",
        fileName: null,
        file: null,
        fileContent: "",
        textArea: "some-text",
        password: "",
        status: "new",
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
    expect(Object.keys(wrapper.emitted())).toContain("cancelEditing");
  });

  it("emits finishEditing with correct data when Save button is clicked", async () => {
    const wrapper = await mountKeyStorageEdit();
    const result = {
      success: true,
      keyDetails: {
        name: "exampleKey",
        path: "/keys/exampleKey",
        keyType: "privateKey",
      },
    };
    jest.spyOn(wrapper.vm as any, "handleUploadKey").mockImplementation(() => {
      wrapper.vm.$emit("finishEditing", result);
    });
    const saveButton = wrapper.find('[data-testid="save-btn"]');
    await wrapper.vm.$nextTick();
    expect(wrapper.emitted().finishEditing).toBeFalsy();
    expect(saveButton.isVisible()).toBe(true);
    await saveButton.trigger("click");
    expect(wrapper.emitted().finishEditing[0]).toEqual([result]);
  });

  it("enables and disables the Save button based on key name input", async () => {
    const wrapper = await mountKeyStorageEdit({
      uploadSetting: { textArea: "" },
    });
    const keyNameInput = wrapper.find('[data-testid="key-name-input"]');
    const saveButton = wrapper.find('[data-testid="save-btn"]');
    // the input is empty so the Save button should be disabled
    expect(saveButton.attributes()).toHaveProperty("disabled");
    expect(saveButton.attributes("disabled")).toBe("");
    // Setting a valid key name which should enable the Save button
    await keyNameInput.setValue("test-key");
    await wrapper.vm.$nextTick();
    expect(saveButton.attributes("disabled")).toBeFalsy();
    // Clear the key name input (set it to an empty string)
    await keyNameInput.setValue("");
    await wrapper.vm.$nextTick();
    expect(saveButton.attributes()).toHaveProperty("disabled"); // Should be disabled again
  });
});
