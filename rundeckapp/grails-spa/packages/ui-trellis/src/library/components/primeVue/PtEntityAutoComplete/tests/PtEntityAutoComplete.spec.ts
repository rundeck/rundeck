import { flushPromises, mount } from "@vue/test-utils";
import AutoComplete from "primevue/autocomplete";
import { PtEntityAutoComplete } from "../../index";

const JOBS = [
  { name: "Deploy Web App", group: "release/prod", id: "uuid-1" },
  { name: "Restart", group: "", id: "uuid-2" },
];

const createWrapper = async (props = {}): Promise<any> => {
  const wrapper = mount(PtEntityAutoComplete, {
    props: {
      modelValue: "",
      optionLabel: "name",
      optionSecondary: "group",
      placeholder: "Job name...",
      ...props,
    },
    global: { components: { AutoComplete } },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

/** Simulate PrimeVue firing its complete event for a typed query. */
const typeQuery = async (wrapper: any, query: string) => {
  await wrapper.findComponent(AutoComplete).vm.$emit("complete", { query });
  await flushPromises();
};

describe("PtEntityAutoComplete", () => {
  describe("label", () => {
    it("does not show a label when the label prop is not provided", async () => {
      const wrapper = await createWrapper();
      expect(
        wrapper.find('[data-testid="pt-entity-autocomplete-label"]').exists(),
      ).toBe(false);
    });

    it("shows the label text above the field so users know what to fill", async () => {
      const wrapper = await createWrapper({ label: "Job Name" });
      const label = wrapper.find(
        '[data-testid="pt-entity-autocomplete-label"]',
      );
      expect(label.exists()).toBe(true);
      expect(label.text()).toBe("Job Name");
    });

    it("links the label to the input via the inputId so screen readers work", async () => {
      const wrapper = await createWrapper({
        label: "Job Name",
        inputId: "jobNameField",
      });
      expect(
        wrapper
          .find('[data-testid="pt-entity-autocomplete-label"]')
          .attributes("for"),
      ).toBe("jobNameField");
    });
  });

  describe("error message", () => {
    it("does not show an error when the field is valid", async () => {
      const wrapper = await createWrapper({
        invalid: false,
        errorText: "Job is required",
      });
      expect(
        wrapper.find('[data-testid="pt-entity-autocomplete-error"]').exists(),
      ).toBe(false);
    });

    it("shows the error message when the field is invalid so users know what went wrong", async () => {
      const wrapper = await createWrapper({
        invalid: true,
        errorText: "Job is required",
      });
      const error = wrapper.find(
        '[data-testid="pt-entity-autocomplete-error"]',
      );
      expect(error.exists()).toBe(true);
      expect(error.text()).toBe("Job is required");
    });
  });

  describe("loading suggestions", () => {
    it("shows the entities returned for the typed query", async () => {
      const search = jest.fn().mockResolvedValue(JOBS);
      const wrapper = await createWrapper({ search });

      await typeQuery(wrapper, "Depl");

      expect(search).toHaveBeenCalledWith("Depl");
      expect(wrapper.findComponent(AutoComplete).props("suggestions")).toEqual(
        JOBS,
      );
    });

    it("keeps entity names containing spaces intact so multi-word jobs can be found", async () => {
      const search = jest.fn().mockResolvedValue(JOBS);
      const wrapper = await createWrapper({ search });

      await typeQuery(wrapper, "Deploy Web");

      expect(search).toHaveBeenCalledWith("Deploy Web");
      const suggestions = wrapper
        .findComponent(AutoComplete)
        .props("suggestions");
      expect(suggestions).toContainEqual(JOBS[0]);
    });

    it("loads suggestions for an empty query so the list opens on focus", async () => {
      const search = jest.fn().mockResolvedValue(JOBS);
      const wrapper = await createWrapper({ search, minChars: 0 });

      await typeQuery(wrapper, "");

      expect(search).toHaveBeenCalledWith("");
    });

    it("does not search until the query reaches minChars", async () => {
      const search = jest.fn().mockResolvedValue(JOBS);
      const wrapper = await createWrapper({ search, minChars: 3 });

      await typeQuery(wrapper, "De");

      expect(search).not.toHaveBeenCalled();
      expect(wrapper.findComponent(AutoComplete).props("suggestions")).toEqual(
        [],
      );
    });

    it("shows no suggestions when no search callback is configured", async () => {
      const wrapper = await createWrapper();

      await typeQuery(wrapper, "Depl");

      expect(wrapper.findComponent(AutoComplete).props("suggestions")).toEqual(
        [],
      );
    });

    it("shows the newest query's results even when an earlier search resolves later", async () => {
      let resolveFirst: (v: unknown) => void = () => {};
      const first = new Promise((resolve) => {
        resolveFirst = resolve;
      });
      const search = jest
        .fn()
        .mockReturnValueOnce(first)
        .mockResolvedValueOnce([JOBS[1]]);
      const wrapper = await createWrapper({ search });

      // First (slow) query, then a second one that resolves immediately.
      wrapper.findComponent(AutoComplete).vm.$emit("complete", { query: "D" });
      await typeQuery(wrapper, "Re");

      // The stale first response arrives last and must be discarded.
      resolveFirst([JOBS[0]]);
      await flushPromises();

      expect(wrapper.findComponent(AutoComplete).props("suggestions")).toEqual([
        JOBS[1],
      ]);
    });

    it("shows no suggestions when the search fails", async () => {
      jest.spyOn(console, "error").mockImplementation(() => {});
      const search = jest.fn().mockRejectedValue(new Error("boom"));
      const wrapper = await createWrapper({ search });

      await typeQuery(wrapper, "Depl");

      expect(wrapper.findComponent(AutoComplete).props("suggestions")).toEqual(
        [],
      );
      (console.error as jest.Mock).mockRestore();
    });
  });

  describe("selecting an entity", () => {
    it("emits the whole entity so the caller can populate related fields", async () => {
      const wrapper = await createWrapper();

      await wrapper
        .findComponent(AutoComplete)
        .vm.$emit("option-select", { value: JOBS[0] });

      expect(wrapper.emitted("select")).toHaveLength(1);
      expect(wrapper.emitted("select")![0]).toEqual([JOBS[0]]);
    });

    it("emits the entity's label as the text value rather than the object", async () => {
      const wrapper = await createWrapper();

      await wrapper
        .findComponent(AutoComplete)
        .vm.$emit("update:modelValue", JOBS[0]);

      expect(wrapper.emitted("update:modelValue")![0]).toEqual([
        "Deploy Web App",
      ]);
    });

    it("emits typed text unchanged so free-form values are preserved", async () => {
      const wrapper = await createWrapper();

      await wrapper
        .findComponent(AutoComplete)
        .vm.$emit("update:modelValue", "Some Other Job");

      expect(wrapper.emitted("update:modelValue")![0]).toEqual([
        "Some Other Job",
      ]);
    });
  });

  describe("read only", () => {
    it("marks the input readonly so the value stays visible but cannot be edited", async () => {
      const wrapper = await createWrapper({
        readOnly: true,
        inputTestid: "jobNameField",
      });
      expect(
        wrapper.find('[data-testid="jobNameField"]').attributes("readonly"),
      ).toBeDefined();
    });

    it("leaves the input editable by default", async () => {
      const wrapper = await createWrapper({ inputTestid: "jobNameField" });
      expect(
        wrapper.find('[data-testid="jobNameField"]').attributes("readonly"),
      ).toBeUndefined();
    });

    it("does not load suggestions while readonly", async () => {
      const search = jest.fn().mockResolvedValue(JOBS);
      const wrapper = await createWrapper({ search, readOnly: true });

      await typeQuery(wrapper, "Depl");

      expect(search).not.toHaveBeenCalled();
    });
  });
});
