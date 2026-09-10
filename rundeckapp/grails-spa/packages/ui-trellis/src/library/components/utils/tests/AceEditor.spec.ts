import { describe, it, expect } from "@jest/globals";
import { shallowMount } from "@vue/test-utils";
import AceEditor from "../AceEditor.vue";
import AceEditorVue from "../AceEditorVue.vue";

const createWrapper = (props: Record<string, any> = {}) =>
  shallowMount(AceEditor, {
    props: {
      modelValue: "",
      ...props,
    },
  });

describe("AceEditor", () => {
  describe("minLines/maxLines forwarding", () => {
    it("forwards default minLines/maxLines to the underlying ace editor", () => {
      const wrapper = createWrapper();

      const ace = wrapper.findComponent(AceEditorVue);
      expect(ace.props("minLines")).toBe(12);
      expect(ace.props("maxLines")).toBe(Infinity);
    });

    it("forwards custom minLines/maxLines props to the underlying ace editor", () => {
      const wrapper = createWrapper({ minLines: 1, maxLines: 50 });

      const ace = wrapper.findComponent(AceEditorVue);
      expect(ace.props("minLines")).toBe(1);
      expect(ace.props("maxLines")).toBe(50);
    });
  });
});
