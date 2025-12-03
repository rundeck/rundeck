import { mount, flushPromises } from "@vue/test-utils";
import ProjectPicker from "../ProjectPicker.vue";
import { api } from "../../../services/api";

jest.mock("../../../services/api", () => ({
  api: {
    get: jest.fn() as jest.Mock,
  },
}));

jest.mock("../../../rundeckService", () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    rdBase: "http://localhost:4440/",
    projectName: "testProject",
    apiVersion: "44",
  })),
}));

(api.get as jest.Mock).mockResolvedValue({
  data: [{ name: "Project A" }, { name: "Project B" }],
});
const createWrapper = async () => {
  const wrapper = mount(ProjectPicker, {
    props: {
      modelValue: "",
    },
  });
  await flushPromises();
  return wrapper;
};
describe("ProjectPicker.vue", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });
  it("renders project options after fetching project list", async () => {
    const wrapper = await createWrapper();
    await flushPromises();
    const options = wrapper.findAll('[data-testid="project-option"]');
    expect(options.length).toBe(3);
    expect(options[1].text()).toBe("Project A");
    expect(options[2].text()).toBe("Project B");
  });
  it("emits 'update:modelValue' only when a project is selected", async () => {
    const wrapper = await createWrapper();
    expect(wrapper.emitted("update:modelValue")).toBeFalsy();
    const select = wrapper.find('[data-testid="project-select"]');
    await select.setValue("Project A");
    expect(wrapper.emitted("update:modelValue")).toBeTruthy();
    expect(wrapper.emitted("update:modelValue")[0][0]).toBe("Project A");
  });
  it("renders only the default option when project list is empty", async () => {
    (api.get as jest.Mock).mockResolvedValueOnce({
      data: [],
    });
    const wrapper = await createWrapper();
    await flushPromises();
    const options = wrapper.findAll('[data-testid="project-option"]');
    expect(options.length).toBe(1);
    expect(options[0].text()).toBe("");
  });
});
