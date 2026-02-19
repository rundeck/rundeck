import { mount } from "@vue/test-utils";
import AutoComplete from "primevue/autocomplete";
import { PtAutoComplete } from "../../index";
import type { TabConfig } from "../PtAutoCompleteTypes";

const SUGGESTIONS = [
  { name: "${job.execid}", title: "Execution ID", type: "job" },
  { name: "${job.id}", title: "Job ID", type: "job" },
];

const createWrapper = async (props = {}): Promise<any> => {
  const wrapper = mount(PtAutoComplete, {
    props: {
      modelValue: "",
      suggestions: SUGGESTIONS,
      placeholder: "Type here...",
      ...props,
    },
    global: { components: { AutoComplete } },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("PtAutoComplete", () => {
  describe("label", () => {
    it("does not show a label when the label prop is not provided", async () => {
      const wrapper = await createWrapper();
      expect(wrapper.find('[data-testid="pt-autocomplete-label"]').exists()).toBe(false);
    });

    it("shows the label text above the field so users know what to fill", async () => {
      const wrapper = await createWrapper({ label: "Variable" });
      const label = wrapper.find('[data-testid="pt-autocomplete-label"]');
      expect(label.exists()).toBe(true);
      expect(label.text()).toBe("Variable");
    });

    it("links the label to the input via the inputId so screen readers work", async () => {
      const wrapper = await createWrapper({ label: "Variable", inputId: "var-field" });
      expect(
        wrapper.find('[data-testid="pt-autocomplete-label"]').attributes("for"),
      ).toBe("var-field");
    });
  });

  describe("error message", () => {
    it("does not show an error when the field is valid", async () => {
      const wrapper = await createWrapper({ invalid: false, errorText: "Bad value" });
      expect(wrapper.find('[data-testid="pt-autocomplete-error"]').exists()).toBe(false);
    });

    it("shows the error message text when the field is invalid so users know what went wrong", async () => {
      const wrapper = await createWrapper({ invalid: true, errorText: "Variable not found" });
      const error = wrapper.find('[data-testid="pt-autocomplete-error"]');
      expect(error.exists()).toBe(true);
      expect(error.text()).toBe("Variable not found");
    });

    it("does not show an error even when invalid if no errorText is provided", async () => {
      const wrapper = await createWrapper({ invalid: true });
      expect(wrapper.find('[data-testid="pt-autocomplete-error"]').exists()).toBe(false);
    });
  });

  describe("events emitted toward parent", () => {
    it("notifies the parent of the change event and also syncs v-model when the user commits a value", async () => {
      const wrapper = await createWrapper({ modelValue: "my-value" });
      const changePayload = { value: "my-value" };
      await wrapper.findComponent(AutoComplete).vm.$emit("change", changePayload);
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("onChange")).toHaveLength(1);
      expect(wrapper.emitted("onChange")![0]).toEqual([changePayload]);
      expect(wrapper.emitted("update:modelValue")).toHaveLength(1);
      expect(wrapper.emitted("update:modelValue")![0]).toEqual(["my-value"]);
    });

    it("forwards the complete event with its payload so the parent can track suggestions", async () => {
      const wrapper = await createWrapper();
      const completePayload = { query: "job" };
      await wrapper.findComponent(AutoComplete).vm.$emit("complete", completePayload);
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("onComplete")).toHaveLength(1);
      expect(wrapper.emitted("onComplete")![0]).toEqual([completePayload]);
    });
  });

  describe("suggestion filtering", () => {
    afterEach(() => {
      jest.useRealTimers();
    });

    it("filters suggestions to those matching the typed query so the dropdown shows only relevant options", async () => {
      jest.useFakeTimers();
      const wrapper = await createWrapper();

      // Simulate user typing "execid" – AutoComplete fires complete
      await wrapper.findComponent(AutoComplete).vm.$emit("complete", {
        query: "execid",
        originalEvent: { target: { selectionStart: 6 } },
      });

      jest.advanceTimersByTime(200);
      await wrapper.vm.$nextTick();

      // tabFilteredSuggestions (the massaged prop) should contain only the matching entry
      const suggestions = wrapper.findComponent(AutoComplete).props("suggestions");
      expect(suggestions).toContain("${job.execid}");
      expect(suggestions).not.toContain("${job.id}");
    });

    it("returns no suggestions when the query does not match anything", async () => {
      jest.useFakeTimers();
      const wrapper = await createWrapper();

      await wrapper.findComponent(AutoComplete).vm.$emit("complete", {
        query: "zzz",
        originalEvent: { target: { selectionStart: 3 } },
      });

      jest.advanceTimersByTime(200);
      await wrapper.vm.$nextTick();

      // tabFilteredSuggestions returns undefined for empty results; PrimeVue normalises this to null
      expect(wrapper.findComponent(AutoComplete).props("suggestions")).toBeFalsy();
    });

    it("shows all suggestions when the user types just a dollar sign", async () => {
      jest.useFakeTimers();
      const wrapper = await createWrapper();

      await wrapper.findComponent(AutoComplete).vm.$emit("complete", {
        query: "$",
        originalEvent: { target: { selectionStart: 1 } },
      });

      jest.advanceTimersByTime(200);
      await wrapper.vm.$nextTick();

      const suggestions = wrapper.findComponent(AutoComplete).props("suggestions");
      expect(suggestions).toContain("${job.execid}");
      expect(suggestions).toContain("${job.id}");
    });

    it("restricts suggestions to the active tab's category when tabMode is enabled", async () => {
      jest.useFakeTimers();

      const tabSuggestions = [
        { name: "${job.execid}", title: "Execution ID", type: "job" },
        { name: "${option.myopt}", title: "My Option", type: "option" },
      ];

      const tabs: TabConfig[] = [
        {
          label: "Job",
          filter: (s) => s.type === "job",
          getCount: (s) => s.filter((x) => x.type === "job").length,
        },
        {
          label: "Options",
          filter: (s) => s.type === "option",
          getCount: (s) => s.filter((x) => x.type === "option").length,
        },
      ];

      const wrapper = await createWrapper({ suggestions: tabSuggestions, tabMode: true, tabs });

      // Type "$" to trigger all suggestions – with tabMode the active tab filters the result
      await wrapper.findComponent(AutoComplete).vm.$emit("complete", {
        query: "$",
        originalEvent: { target: { selectionStart: 1 } },
      });

      jest.advanceTimersByTime(200);
      await wrapper.vm.$nextTick();

      // Default active tab is index 0 ("Job") – only job suggestions should be passed to AutoComplete
      const suggestions = wrapper.findComponent(AutoComplete).props("suggestions");
      expect(suggestions).toContain("${job.execid}");
      expect(suggestions).not.toContain("${option.myopt}");
    });
  });
});
