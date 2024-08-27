import { shallowMount, VueWrapper } from "@vue/test-utils";
import DetailsEditor from "../DetailsEditor.vue";
import AceEditor from "@/library/components/utils/AceEditor.vue";
import UiSocket from "@/library/components/utils/UiSocket.vue";
import mitt from "mitt";

jest.mock("@/library", () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    eventBus: mitt(),
  })),
}));

const createWrapper = async (propsData = {}): Promise<VueWrapper<any>> => {
  const wrapper = shallowMount(DetailsEditor, {
    props: {
      allowHtml: true,
      modelValue: {
        jobName: "testJob",
        description: "testDescription",
        groupPath: "testGroup",
      },
      ...propsData,
    },
    global: {
      components: {
        AceEditor,
        UiSocket,
      },
      mocks: {
        $t: (msg: string) => msg,
      },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("DetailsEditor.vue", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it("loads existing data correctly", async () => {
    const wrapper = await createWrapper();
    const inputs = wrapper.findAll("input");
    const expectedValues = ["testJob", "testGroup"];
    inputs.forEach((input, index) => {
      expect(input.element.value).toBe(expectedValues[index]);
    });
    const textarea = wrapper.find("textarea");
    expect(textarea.element.value).toBe("testDescription");
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
        description: "newDescription",
        groupPath: "testGroup/newGroup",
        jobName: "newJobName",
      }),
    ]);
  });

  it('shows the preview tab when description contains "---"', async () => {
    // window.markdeep.format is a global method added from application.js, that calls another third party method,
    // to sanitize the data and calls a callbacks once data is formatted (none of the methods are exported)
    // therefore this test just checks that the preview tab gets rendered once wrapper.vm.preview isn't null anymore
    // @ts-ignore
    window.markdeep = {
      format: jest.fn().mockImplementation(() =>
        Promise.resolve().then(
          () =>
            //@ts-ignore
            (wrapper.vm.preview = `<section class="md"><p></p><p>
first line
</p><hr>
Preview Content
</section>`),
        ),
      ),
    };

    const wrapper = await createWrapper();

    let previewTab = wrapper.find("#previewrunbook");
    let previewContent = wrapper.find("#descpreview_content");

    expect(previewTab.exists()).toBeFalsy();
    expect(previewContent.exists()).toBeFalsy();

    // due to stub, just trigger emit event to update description
    const editor = wrapper.findComponent(AceEditor);
    editor.vm.$emit("update:modelValue", "first line---Preview Content");
    await wrapper.vm.$nextTick();

    previewTab = wrapper.find("#previewrunbook");
    expect(previewTab.exists()).toBeTruthy();

    // click on tab to trigger preview to be populated
    await previewTab.trigger("click");

    await wrapper.vm.$nextTick();
    previewContent = wrapper.find("#descpreview_content");
    expect(previewContent.exists()).toBeTruthy();
  });

  it("shows error class if job name is empty", async () => {
    const wrapper = await createWrapper();
    const input = wrapper.find("#schedJobName");

    await input.setValue("");
    await input.trigger("blur");
    await wrapper.vm.$nextTick();

    const schedJobNameLabel = wrapper.find("#schedJobNameLabel > div");
    expect(schedJobNameLabel.classes("has-error")).toBeTruthy();
  });
});
