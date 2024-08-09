import { mount, VueWrapper } from "@vue/test-utils";
import KeyStorageEdit from "../KeyStorageEdit.vue";

// jest.mock("@/library/rundeckService", () => {
//   return {
//     getRundeckContext: jest.fn().mockReturnValue({
//       rdBase: "mockRdBase",
//       // url: jest.fn().mockReturnValue("http://localhost"),
//       // projectName: "test-project",
//       // eventBus: {
//       //   on: jest.fn(),
//       //   emit: jest.fn(),
//       //   off: jest.fn(),
//       // },
//     }),
//   };
// });
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
});
