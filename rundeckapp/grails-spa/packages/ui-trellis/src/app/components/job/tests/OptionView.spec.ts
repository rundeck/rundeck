import { mount, VueWrapper } from "@vue/test-utils";
import { JobOption } from "../../../../library/types/jobs/JobEdit";
import { Btn, Popover } from "uiv";

import OptionView from "../options/OptionView.vue";

const mountOptionView = async (options: {
  option: JobOption;
  editable: boolean;
}): Promise<VueWrapper<any>> => {
  const wrapper = mount(OptionView, {
    props: {
      option: options.option,
      editable: options.editable,
    },
    global: {
      mocks: {
        $t: jest.fn().mockImplementation((msg) => msg),
        $tc: jest.fn().mockImplementation((msg) => msg),
      },
    },

    components: {
      Popover,
      btn: Btn,
    },
  });

  // Wait for the next Vue tick to allow for asynchronous rendering
  await wrapper.vm.$nextTick();

  return wrapper;
};

describe("OptionView", () => {
  it("text option shows option name", async () => {
    let option = {name: "optionName", optionType: "text"} as JobOption;
    const wrapper = await mountOptionView({ option, editable: true });
    // Wait for the next Vue tick to allow for asynchronous rendering
    await wrapper.vm.$nextTick();

    let detail = wrapper.find(".optdetail");
    expect(detail).toBeTruthy();
    expect(detail.text()).toContain("optionName");
    //has no file icon
    expect(wrapper.find(".glyphicon-file")).toBeFalsy();
  });
  it("file option shows file icon", async () => {
    let option = {name: "optionName", optionType: "file"} as JobOption;
    const wrapper = await mountOptionView({ option, editable: true });
    // Wait for the next Vue tick to allow for asynchronous rendering
    await wrapper.vm.$nextTick();

    let detail = wrapper.find(".optdetail");
    expect(detail).toBeTruthy();
    expect(detail.text()).toContain("optionName");
    //has a file icon
    expect(wrapper.find(".glyphicon-file")).toBeTruthy();
  });
});
