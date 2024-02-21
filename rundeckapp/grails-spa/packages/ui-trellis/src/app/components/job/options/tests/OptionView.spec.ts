import { mount, VueWrapper } from "@vue/test-utils";
import { JobOption } from "../../../../../library/types/jobs/JobEdit";
import { Btn, Popover } from "uiv";

import OptionView from "../OptionView.vue";

const PopoverStub = {
  name: "Popover",
  props: ["trigger", "placement"],
  template:
    "<div id='popover-test-wrapper'><slot></slot><slot name='popover'></slot></div>",
};
const mountOptionView = async (options: {
  option: any;
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
      stubs: {
        Popover: PopoverStub,
      },
    },

    components: {
      btn: Btn,
    },
  });

  // Wait for the next Vue tick to allow for asynchronous rendering
  await wrapper.vm.$nextTick();

  return wrapper;
};

describe("OptionView", () => {
  it("shows option name", async () => {
    let option = { name: "optionName", optionType: "text" } as JobOption;
    const wrapper = await mountOptionView({ option, editable: true });

    let detail = wrapper.find(".optdetail span.optdetail_name");
    expect(detail.exists()).toBeTruthy();
    expect(detail.text()).toContain("optionName");
    //has no file icon
    expect(wrapper.find(".glyphicon-file").exists()).toBeFalsy();
  });
  it("description in title", async () => {
    let option = {
      name: "optionName",
      optionType: "text",
      description: "optionDescription",
    } as JobOption;
    const wrapper = await mountOptionView({ option, editable: true });

    let reqspan = wrapper.find(".optdetail span.optdetail_name");
    expect(reqspan.exists()).toBeTruthy();
    expect(reqspan.attributes().title).toContain("optionDescription");
  });
  it("required title", async () => {
    let option = {
      name: "optionName",
      optionType: "text",
      description: "optionDescription",
      required: true,
    } as JobOption;
    const wrapper = await mountOptionView({ option, editable: true });

    let reqspan = wrapper.find(".optdetail span.optdetail_name");
    expect(reqspan.exists()).toBeTruthy();
    expect(reqspan.attributes().title).toContain("optionDescription");
    expect(reqspan.attributes().title).toContain("option.view.required.title");
  });
  it("file type shows file icon", async () => {
    let option = { name: "optionName", optionType: "file" } as JobOption;
    const wrapper = await mountOptionView({ option, editable: true });

    let detail = wrapper.find(".optdetail span.optdetail_name");
    expect(detail.exists()).toBeTruthy();
    expect(detail.text()).toContain("optionName");
    //has a file icon
    expect(wrapper.find(".glyphicon-file").exists()).toBeTruthy();
  });
  it("secure with key storage shows lock icon", async () => {
    let option = {
      name: "optionName",
      secure: true,
      defaultStoragePath: "path",
    } as JobOption;
    const wrapper = await mountOptionView({ option, editable: true });

    let detail = wrapper.find(".optdetail > span.optdetail_default_value");
    expect(detail.exists()).toBeTruthy();
    //has a lock icon
    expect(wrapper.find(".glyphicon-lock").exists()).toBeTruthy();
  });
  it("multivalued shows + text", async () => {
    let option = {
      name: "optionName",
      multivalued: true,
    } as JobOption;
    const wrapper = await mountOptionView({ option, editable: true });

    let detail = wrapper.find(".optdetail > span.optdetail_default_value");
    expect(detail.exists()).toBeTruthy();
    expect(detail.text()).toContain("(+)");
  });
  it("values list", async () => {
    let values = ["avalue", "bvalue"];
    let len = values.length;
    let option = {
      name: "optionName",
      values,
    } as JobOption;
    const wrapper = await mountOptionView({ option, editable: true });
    let popover = wrapper.get("#popover-test-wrapper");
    expect(popover).toBeDefined();
    let detail = popover.get(".valuesSet > .valueslist");
    expect(detail.text()).toEqual("option.values.c");
    let note = popover.get(".info.note");
    expect(note.text()).toEqual("option.view.allowedValues.label");
    let items = popover.findAll(".valueItem");
    expect(items.length).toEqual(len);
    for (let i = 0; i < len; i++) {
      expect(items[i].text()).toEqual(values[i]);
    }
  });
  it("values url placeholder", async () => {
    let option = {
      name: "optionName",
      valuesUrl: "aurl",
    } as JobOption;
    const wrapper = await mountOptionView({ option, editable: true });
    let detail = wrapper.find(".valuesSet > .valuesUrl");
    expect(detail.exists()).toBeTruthy();
    expect(detail.text()).toContain("option.view.valuesUrl.placeholder");
    expect(detail.attributes().title).toContain("option.view.valuesUrl.title");
  });
  it("enforced indicator", async () => {
    let option = {
      name: "optionName",
      enforced: true,
    } as JobOption;
    const wrapper = await mountOptionView({ option, editable: true });
    let detail = wrapper.find(".enforceSet > .enforced");
    expect(detail.exists()).toBeTruthy();
    expect(detail.text()).toContain("option.view.enforced.placeholder");
    expect(detail.attributes().title).toContain("option.view.enforced.title");
  });
  it("regex indicator", async () => {
    let regex = "someregexvalue";
    let option = {
      name: "optionName",
      regex,
    } as JobOption;
    const wrapper = await mountOptionView({ option, editable: true });

    let detail = wrapper.find(".enforceSet .regex");
    expect(detail.exists()).toBeTruthy();
    expect(detail.text()).toContain(regex);
    expect(detail.attributes()["data-role"]).toEqual("trigger");
    let note = wrapper.find(".enforceSet .info.note");
    expect(note.exists()).toBeTruthy();
    expect(note.text()).toEqual("option.view.regex.info.note");
    let code = wrapper.find(".enforceSet code");
    expect(code.exists()).toBeTruthy();
    expect(code.text()).toEqual(regex);
  });
  it("not enforced or regex text", async () => {
    let option = {
      name: "optionName",
    } as JobOption;
    const wrapper = await mountOptionView({ option, editable: true });
    let detail = wrapper.find(".enforceSet > .any");
    expect(detail.exists()).toBeTruthy();
    expect(detail.text()).toContain("option.view.notenforced.placeholder");
    expect(detail.attributes().title).toContain(
      "option.view.notenforced.title",
    );
  });
  it.each([1, 10, 15, 20])(
    "default value len %p is not trucated",
    async (len: number) => {
      let value = "a".repeat(len);
      let option = {
        name: "optionName",
        value,
      } as JobOption;
      const wrapper = await mountOptionView({ option, editable: true });
      expect(wrapper.vm.displayDefaultValueTruncated).toEqual(value);
      expect(wrapper.vm.displayDefaultValue).toEqual(value);
      let detail = wrapper.find(
        ".optdetail > span.optdetail_default_value > span::first-child",
      );
      expect(detail.exists()).toBeTruthy();
      expect(detail.text()).toEqual(value);
      expect(detail.attributes().title).toEqual(value);
    },
  );
  it.each([21, 50])("default value len %p is trucated", async (len: number) => {
    let value = "a".repeat(len);
    let option = {
      name: "optionName",
      value,
    } as JobOption;
    const wrapper = await mountOptionView({ option, editable: true });
    expect(wrapper.vm.displayDefaultValue).toEqual(value);
    expect(wrapper.vm.displayDefaultValueTruncated).toEqual(
      value.substring(0, 20) + "...",
    );
    let detail = wrapper.find(
      ".optdetail > span.optdetail_default_value > span::first-child",
    );
    expect(detail.exists()).toBeTruthy();
    expect(detail.text()).toEqual(value.substring(0, 20) + "...");
    expect(detail.attributes().title).toEqual(value);
  });
  it.each([5, 10, 100])(
    "description with %p chars is not truncated",
    async (repeat: number) => {
      let description = "a".repeat(repeat);
      let option = {
        name: "optionName",
        description,
      } as JobOption;
      const wrapper = await mountOptionView({ option, editable: true });

      expect(wrapper.vm.truncatedDescription).toEqual(description);

      let detail = wrapper.find(".optdetail > span.opt-desc");
      expect(detail.exists()).toBeTruthy();
      expect(detail.text()).toEqual(description);
    },
  );
  it.each([101])(
    "description with %p chars is truncated",
    async (repeat: number) => {
      let description = "a".repeat(repeat);
      let option = {
        name: "optionName",
        description,
      } as JobOption;
      const wrapper = await mountOptionView({ option, editable: true });

      expect(wrapper.vm.truncatedDescription).toEqual(
        description.substring(0, 100),
      );

      let detail = wrapper.find(".optdetail > span.opt-desc");
      expect(detail.exists()).toBeTruthy();
      expect(detail.text()).toEqual(description.substring(0, 100));
    },
  );
});
