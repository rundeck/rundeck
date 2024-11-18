import { mount, flushPromises } from "@vue/test-utils";
import ProjectPicker from "../ProjectPicker.vue";
import { client } from "../../../modules/rundeckClient";
jest.mock("../../../modules/rundeckClient", () => ({
  client: {
    projectList: jest.fn().mockResolvedValue([]),
  },
}));
const createWrapper = async () => {
  const wrapper = mount(ProjectPicker, {
    props: {
      modelValue: "",
    },
    attachTo: document.body,
  });
  await flushPromises();
  return wrapper;
};
describe("ProjectPicker.vue", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });
  it("loads and displays project options on mount", async () => {
    (client.projectList as jest.Mock).mockResolvedValueOnce([
      { name: "Project A" },
      { name: "Project B" },
    ]);
    const wrapper = await createWrapper();
    await flushPromises();
    const options = wrapper.findAll('[data-testid="project-option"]');
    expect(options.length).toBe(3);
    expect(options[1].text()).toBe("Project A");
    expect(options[2].text()).toBe("Project B");
  });
  it("updates the selected project when a new option is selected", async () => {
    (client.projectList as jest.Mock).mockResolvedValueOnce([
      { name: "Project A" },
      { name: "Project B" },
    ]);
    const wrapper = await createWrapper();
    await flushPromises();
    const select = wrapper.find('[data-testid="project-select"]');
    await select.setValue("Project A");
    expect(wrapper.emitted("update:modelValue")).toBeTruthy();
    expect(wrapper.emitted("update:modelValue")[0][0]).toBe("Project A");
  });
  it("reacts to user selecting a new value from the dropdown", async () => {
    (client.projectList as jest.Mock).mockResolvedValueOnce([
      { name: "Project A" },
      { name: "Project B" },
    ]);
    const wrapper = await createWrapper();
    await flushPromises();
    const select = wrapper.find('[data-testid="project-select"]');
    await select.setValue("Project B");
    await flushPromises();
    expect(wrapper.emitted("update:modelValue")[0][0]).toBe("Project B");
  });
  it("renders no options when the project list is empty", async () => {
    (client.projectList as jest.Mock).mockResolvedValueOnce([]);
    const wrapper = await createWrapper();
    await flushPromises();
    const options = wrapper.findAll('[data-testid="project-option"]');
    expect(options.length).toBe(1);
    expect(options[0].text()).toBe("");
  });
  it("does not emit update:modelValue until a user selects a value", async () => {
    (client.projectList as jest.Mock).mockResolvedValueOnce([
      { name: "Project A" },
      { name: "Project B" },
    ]);
    const wrapper = await createWrapper();
    await flushPromises();
    expect(wrapper.emitted("update:modelValue")).toBeFalsy();
    const select = wrapper.find('[data-testid="project-select"]');
    await select.setValue("Project A");
    expect(wrapper.emitted("update:modelValue")).toBeTruthy();
    expect(wrapper.emitted("update:modelValue")[0][0]).toBe("Project A");
  });
});
