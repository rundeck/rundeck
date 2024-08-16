import { mount, VueWrapper } from "@vue/test-utils";
import KeyStorageEdit from "../KeyStorageEdit.vue";
type KeyStorageEditType = InstanceType<typeof KeyStorageEdit>;
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
        textArea: "",
        password: "",
        status: "new",
        errorMsg: null,
        dontOverwrite: false,
      },
      project: "test-project",
      rootPath: "keys/project/test-project",
      ...props,
    },
  });
};
describe("KeyStorageEdit", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });
  it("renders the key storage ", async () => {
    const wrapper = await mountKeyStorageEdit();
    expect(wrapper.exists()).toBe(true);
  });
  it("emits cancelEditing when cancel button is clicked", async () => {
    const wrapper = await mountKeyStorageEdit();
    const cancelButton = wrapper.find("button.btn-default");
    await cancelButton.trigger("click");
    expect(wrapper.emitted().cancelEditing).toBeTruthy();
  });
  it("emits finishEditing with correct data when Save button is clicked", async () => {
    const wrapper = await mountKeyStorageEdit();
    const vm = wrapper.vm as KeyStorageEditType;
    await wrapper.vm.$nextTick();
    const handleUploadKeySpy = jest
      .spyOn(vm, "handleUploadKey")
      .mockImplementation(async () => {
        vm.$emit("finishEditing", "result");
      });
    const saveButton = wrapper.find("button.btn-cta");
    await (wrapper.vm as any).handleUploadKey();
    await wrapper.vm.$nextTick();
    await saveButton.trigger("click");
    await wrapper.vm.$nextTick();
    expect(handleUploadKeySpy).toHaveBeenCalled();
    expect(wrapper.emitted().finishEditing).toBeTruthy();
    expect(wrapper.emitted().finishEditing[0]).toEqual(["result"]);
  });
});
