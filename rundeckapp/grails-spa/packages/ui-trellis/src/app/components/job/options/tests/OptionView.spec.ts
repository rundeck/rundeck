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
    const option = { name: "optionName", type: "text" } as JobOption;
    const wrapper = await mountOptionView({ option, editable: true });

    const detail = wrapper.find(".optdetail span.optdetail_name");
    expect(detail.exists()).toBeTruthy();
    expect(detail.text()).toContain("optionName");
    //has no file icon
    expect(wrapper.find(".glyphicon-file").exists()).toBeFalsy();
  });
  it("description in title", async () => {
    const option = {
      name: "optionName",
      type: "text",
      description: "optionDescription",
    } as JobOption;
    const wrapper = await mountOptionView({ option, editable: true });

    const reqspan = wrapper.find(".optdetail span.optdetail_name");
    expect(reqspan.exists()).toBeTruthy();
    expect(reqspan.attributes().title).toContain("optionDescription");
  });
  it("required title", async () => {
    const option = {
      name: "optionName",
      type: "text",
      description: "optionDescription",
      required: true,
    } as JobOption;
    const wrapper = await mountOptionView({ option, editable: true });

    const reqspan = wrapper.find(".optdetail span.optdetail_name");
    expect(reqspan.exists()).toBeTruthy();
    expect(reqspan.attributes().title).toContain("optionDescription");
    expect(reqspan.attributes().title).toContain("option.view.required.title");
  });
  it("file type shows file icon", async () => {
    const option = { name: "optionName", type: "file" } as JobOption;
    const wrapper = await mountOptionView({ option, editable: true });

    const detail = wrapper.find(".optdetail span.optdetail_name");
    expect(detail.exists()).toBeTruthy();
    expect(detail.text()).toContain("optionName");
    //has a file icon
    expect(wrapper.find(".glyphicon-file").exists()).toBeTruthy();
  });
  it("secure with key storage shows lock icon", async () => {
    const option = {
      name: "optionName",
      secure: true,
      storagePath: "path",
    } as JobOption;
    const wrapper = await mountOptionView({ option, editable: true });

    const detail = wrapper.find(".optdetail > span.optdetail_default_value");
    expect(detail.exists()).toBeTruthy();
    //has a lock icon
    expect(wrapper.find(".glyphicon-lock").exists()).toBeTruthy();
  });
  it("multivalued shows + text", async () => {
    const option = {
      name: "optionName",
      multivalued: true,
    } as JobOption;
    const wrapper = await mountOptionView({ option, editable: true });

    const detail = wrapper.find(".optdetail > span.optdetail_default_value");
    expect(detail.exists()).toBeTruthy();
    expect(detail.text()).toContain("(+)");
  });
  it("values list", async () => {
    const values = ["avalue", "bvalue"];
    const len = values.length;
    const option = {
      name: "optionName",
      values,
    } as JobOption;
    const wrapper = await mountOptionView({ option, editable: true });
    const popover = wrapper.get("#popover-test-wrapper");
    expect(popover).toBeDefined();
    const detail = popover.get(".valuesSet > .valueslist");
    expect(detail.text()).toEqual("option.values.c");
    const note = popover.get(".info.note");
    expect(note.text()).toEqual("option.view.allowedValues.label");
    const items = popover.findAll(".valueItem");
    expect(items.length).toEqual(len);
    for (let i = 0; i < len; i++) {
      expect(items[i].text()).toEqual(values[i]);
    }
  });
  it("values url placeholder", async () => {
    const option = {
      name: "optionName",
      valuesUrl: "aurl",
    } as JobOption;
    const wrapper = await mountOptionView({ option, editable: true });
    const detail = wrapper.find(".valuesSet > .valuesUrl");
    expect(detail.exists()).toBeTruthy();
    expect(detail.text()).toContain("option.view.valuesUrl.placeholder");
    expect(detail.attributes().title).toContain("option.view.valuesUrl.title");
  });
  it("enforced indicator", async () => {
    const option = {
      name: "optionName",
      enforced: true,
    } as JobOption;
    const wrapper = await mountOptionView({ option, editable: true });
    const detail = wrapper.find(".enforceSet > .enforced");
    expect(detail.exists()).toBeTruthy();
    expect(detail.text()).toContain("option.view.enforced.placeholder");
    expect(detail.attributes().title).toContain("option.view.enforced.title");
  });
  it("regex indicator", async () => {
    const regex = "someregexvalue";
    const option = {
      name: "optionName",
      regex,
    } as JobOption;
    const wrapper = await mountOptionView({ option, editable: true });

    const detail = wrapper.find(".enforceSet .regex");
    expect(detail.exists()).toBeTruthy();
    expect(detail.text()).toContain(regex);
    expect(detail.attributes()["data-role"]).toEqual("trigger");
    const note = wrapper.find(".enforceSet .info.note");
    expect(note.exists()).toBeTruthy();
    expect(note.text()).toEqual("option.view.regex.info.note");
    const code = wrapper.find(".enforceSet code");
    expect(code.exists()).toBeTruthy();
    expect(code.text()).toEqual(regex);
  });
  it("not enforced or regex text", async () => {
    const option = {
      name: "optionName",
    } as JobOption;
    const wrapper = await mountOptionView({ option, editable: true });
    const detail = wrapper.find(".enforceSet > .any");
    expect(detail.exists()).toBeTruthy();
    expect(detail.text()).toContain("option.view.notenforced.placeholder");
    expect(detail.attributes().title).toContain(
      "option.view.notenforced.title",
    );
  });
  it.each([1, 10, 15, 20])(
    "default value len %p is not trucated",
    async (len: number) => {
      const value = "a".repeat(len);
      const option = {
        name: "optionName",
        value,
      } as JobOption;
      const wrapper = await mountOptionView({ option, editable: true });
      expect(wrapper.vm.displayDefaultValueTruncated).toEqual(value);
      expect(wrapper.vm.displayDefaultValue).toEqual(value);
      const detail = wrapper.find(
        ".optdetail > span.optdetail_default_value > span::first-child",
      );
      expect(detail.exists()).toBeTruthy();
      expect(detail.text()).toEqual(value);
      expect(detail.attributes().title).toEqual(value);
    },
  );
  it.each([21, 50])("default value len %p is trucated", async (len: number) => {
    const value = "a".repeat(len);
    const option = {
      name: "optionName",
      value,
    } as JobOption;
    const wrapper = await mountOptionView({ option, editable: true });
    expect(wrapper.vm.displayDefaultValue).toEqual(value);
    expect(wrapper.vm.displayDefaultValueTruncated).toEqual(
      value.substring(0, 20) + "...",
    );
    const detail = wrapper.find(
      ".optdetail > span.optdetail_default_value > span::first-child",
    );
    expect(detail.exists()).toBeTruthy();
    expect(detail.text()).toEqual(value.substring(0, 20) + "...");
    expect(detail.attributes().title).toEqual(value);
  });
  it.each([5, 10, 100])(
    "description with %p chars is not truncated",
    async (repeat: number) => {
      const description = "a".repeat(repeat);
      const option = {
        name: "optionName",
        description,
      } as JobOption;
      const wrapper = await mountOptionView({ option, editable: true });

      expect(wrapper.vm.truncatedDescription).toEqual(description);

      const detail = wrapper.find(".optdetail > span.opt-desc");
      expect(detail.exists()).toBeTruthy();
      expect(detail.text()).toEqual(description);
    },
  );
  it.each([101])(
    "description with %p chars is truncated",
    async (repeat: number) => {
      const description = "a".repeat(repeat);
      const option = {
        name: "optionName",
        description,
      } as JobOption;
      const wrapper = await mountOptionView({ option, editable: true });

      expect(wrapper.vm.truncatedDescription).toEqual(
        description.substring(0, 100),
      );

      const detail = wrapper.find(".optdetail > span.opt-desc");
      expect(detail.exists()).toBeTruthy();
      expect(detail.text()).toEqual(description.substring(0, 100));
    },
  );
});
