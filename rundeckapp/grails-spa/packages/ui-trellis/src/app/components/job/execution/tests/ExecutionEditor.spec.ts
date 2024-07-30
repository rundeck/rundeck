import { flushPromises, mount, VueWrapper } from "@vue/test-utils";
import ExecutionEditor from "../ExecutionEditor.vue";
import AceEditor from "@/library/components/utils/AceEditor.vue";
import { executionLifecycle, pluginsInitialData } from "./mocks";

jest.mock("@/library/modules/rundeckClient", () => ({
  client: {},
}));

const createWrapper = async (propsData = {}): Promise<VueWrapper<any>> => {
  const wrapper = mount(ExecutionEditor, {
    props: {
      modelValue: {
        ExecutionLifecycle: {},
      },
      ...propsData,
    },
    global: {
      components: {
        AceEditor,
      },
      mocks: {
        $t: (msg: string) => msg,
      },
    },
  });
  await flushPromises();
  await wrapper.vm.$nextTick();

  return wrapper;
};

describe("ExecutionEditor", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it("loads existing data correctly", async () => {
    const wrapper = await createWrapper();

    await wrapper.setProps({
      initialData: {
        pluginsInitialData,
        ExecutionLifecycle: executionLifecycle,
      },
    });
    await wrapper.vm.$nextTick();

    // const checkboxes = wrapper.findAll("input[type='checkbox']");

    // expect(wrapper.html()).toEqual("");
    // const textarea = wrapper.find("textarea");
    // expect(textarea.element.value).toBe("testDescription");
  });

  it("emits update:modelValue upon updating fields", async () => {
    const wrapper = await createWrapper();
    const input = wrapper.find("#schedJobName");
    await input.setValue("newJobName");

    wrapper.vm.eventBus.emit("group-selected", "testGroup/newGroup");

    const editor = wrapper.findComponent(AceEditor);
    editor.vm.$emit("update:modelValue", "newDescription");

    await wrapper.vm.$nextTick();

    expect(wrapper.emitted("update:modelValue")).toBeTruthy();
    expect(wrapper.emitted("update:modelValue")[0]).toEqual([
      expect.objectContaining({
        ExecutionLifecycle: {},
      }),
    ]);
  });
});
