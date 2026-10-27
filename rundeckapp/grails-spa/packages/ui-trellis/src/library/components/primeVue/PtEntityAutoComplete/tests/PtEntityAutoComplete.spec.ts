import { flushPromises, mount } from "@vue/test-utils";
import { defineComponent, h } from "vue";
import AutoComplete from "primevue/autocomplete";
import { PtEntityAutoComplete } from "../../index";
import type { EntitySuggestion } from "../PtEntityAutoCompleteTypes";

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

interface SelectedItemsSlotProps {
  items: EntitySuggestion[];
  remove: (item: EntitySuggestion) => void;
}

interface OptionSlotProps {
  query: string;
}

const createMultipleWrapper = async (
  modelValue: EntitySuggestion[] = [],
  extraProps: Record<string, unknown> = {},
): Promise<any> => {
  const wrapper = mount(PtEntityAutoComplete, {
    props: {
      modelValue,
      multiple: true,
      dataKey: "id",
      optionLabel: "name",
      inputTestid: "entity-search-input",
      ...extraProps,
    },
    slots: {
      "selected-items": ({ items, remove }: SelectedItemsSlotProps) =>
        h(
          "div",
          { "data-testid": "selected-items" },
          items.map((item) =>
            h(
              "button",
              {
                "data-testid": `selected-item-${item.id}`,
                onClick: () => remove(item),
              },
              String(item.name),
            ),
          ),
        ),
    },
    global: { components: { AutoComplete } },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
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
      expect(
        wrapper.findComponent(AutoComplete).props("suggestions"),
      ).toBeFalsy();
    });

    it("shows no suggestions when no search callback is configured", async () => {
      const wrapper = await createWrapper();

      await typeQuery(wrapper, "Depl");

      expect(
        wrapper.findComponent(AutoComplete).props("suggestions"),
      ).toBeFalsy();
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

    it("stays empty when the user deletes back below minChars while a search is in flight", async () => {
      let resolvePending: (v: unknown) => void = () => {};
      const pending = new Promise((resolve) => {
        resolvePending = resolve;
      });
      const search = jest.fn().mockReturnValueOnce(pending);
      const wrapper = await createWrapper({ search, minChars: 3 });

      // Query long enough to search, then shortened back below the minimum.
      wrapper
        .findComponent(AutoComplete)
        .vm.$emit("complete", { query: "Dep" });
      await typeQuery(wrapper, "De");

      // The response for the abandoned query must not repopulate the list.
      resolvePending(JOBS);
      await flushPromises();

      expect(
        wrapper.findComponent(AutoComplete).props("suggestions"),
      ).toBeFalsy();
    });

    it("shows no suggestions when the search fails", async () => {
      jest.spyOn(console, "error").mockImplementation(() => {});
      const search = jest.fn().mockRejectedValue(new Error("boom"));
      const wrapper = await createWrapper({ search });

      await typeQuery(wrapper, "Depl");

      expect(
        wrapper.findComponent(AutoComplete).props("suggestions"),
      ).toBeFalsy();
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

  describe("multiple selection", () => {
    it("provides the current query to a custom option slot", async () => {
      const AutoCompleteStub = defineComponent({
        name: "AutoComplete",
        template: `
          <div>
            <slot name="option" :option="{ name: 'api-01' }" :index="0" />
          </div>
        `,
      });
      const wrapper = mount(PtEntityAutoComplete, {
        props: {
          modelValue: [],
          multiple: true,
          optionLabel: "name",
        },
        slots: {
          option: ({ query }: OptionSlotProps) =>
            h("span", { "data-testid": "option-query" }, query),
        },
        global: { stubs: { AutoComplete: AutoCompleteStub } },
      });

      await wrapper
        .findComponent(AutoCompleteStub)
        .vm.$emit("update:modelValue", "api");
      await wrapper.vm.$nextTick();

      expect(wrapper.find('[data-testid="option-query"]').text()).toBe("api");
    });

    it("adds an entity without replacing the typed query model", async () => {
      const wrapper = await createMultipleWrapper([JOBS[0]]);

      await wrapper
        .findComponent(AutoComplete)
        .vm.$emit("option-select", { value: JOBS[1] });

      expect(wrapper.emitted("update:modelValue")![0]).toEqual([
        [JOBS[0], JOBS[1]],
      ]);
      expect(wrapper.emitted("select")![0]).toEqual([JOBS[1]]);
    });

    it("keeps the selected entity array unchanged while a query is typed", async () => {
      const wrapper = await createMultipleWrapper([JOBS[0]]);

      await wrapper
        .findComponent(AutoComplete)
        .vm.$emit("update:modelValue", "Restart");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("update:modelValue")).toBeUndefined();
      expect(
        (
          wrapper.find('[data-testid="entity-search-input"]')
            .element as HTMLInputElement
        ).value,
      ).toBe("Restart");
    });

    it("renders selected entities through the selected-items slot", async () => {
      const wrapper = await createMultipleWrapper(JOBS);

      expect(wrapper.find('[data-testid="selected-items"]').text()).toContain(
        "Deploy Web App",
      );
      expect(wrapper.find('[data-testid="selected-items"]').text()).toContain(
        "Restart",
      );
    });

    it("removes an entity through the selected-items slot callback", async () => {
      const wrapper = await createMultipleWrapper(JOBS);

      await wrapper
        .find('[data-testid="selected-item-uuid-1"]')
        .trigger("click");

      expect(wrapper.emitted("update:modelValue")![0]).toEqual([[JOBS[1]]]);
      expect(wrapper.emitted("unselect")![0]).toEqual([JOBS[0]]);
    });

    it("uses dataKey identity when refreshed results represent an existing selection", async () => {
      const wrapper = await createMultipleWrapper([JOBS[0]]);

      await wrapper.findComponent(AutoComplete).vm.$emit("option-select", {
        value: { ...JOBS[0], name: "Renamed in refreshed results" },
      });

      expect(wrapper.emitted("update:modelValue")![0]).toEqual([[]]);
      expect(wrapper.emitted("unselect")![0]).toEqual([JOBS[0]]);
    });

    it("restores the typed query in the input when PrimeVue emits a selected object", async () => {
      const wrapper = await createMultipleWrapper([JOBS[0]]);
      const autoComplete = wrapper.findComponent(AutoComplete);

      await autoComplete.vm.$emit("update:modelValue", "Depl");
      await wrapper.vm.$nextTick();
      (
        autoComplete.vm as unknown as { writeValue: (v: unknown) => void }
      ).writeValue(JOBS[1]);
      await wrapper.vm.$nextTick();

      expect(
        (
          wrapper.find('[data-testid="entity-search-input"]')
            .element as HTMLInputElement
        ).value,
      ).toBe("Depl");
    });

    it("forwards a functional optionLabel resolver to PrimeVue AutoComplete", async () => {
      const labelResolver = (option: EntitySuggestion) =>
        `Job: ${String(option.name)}`;
      const wrapper = await createMultipleWrapper([], {
        optionLabel: labelResolver,
      });

      expect(wrapper.findComponent(AutoComplete).props("optionLabel")).toBe(
        labelResolver,
      );
    });

    it("defers reopening the overlay until after PrimeVue schedules its native hide", async () => {
      jest.useFakeTimers();
      try {
        const wrapper = await createMultipleWrapper([], {
          keepOpenOnSelect: true,
        });
        const autoComplete = wrapper.findComponent(AutoComplete);
        const order: string[] = [];
        jest
          .spyOn(autoComplete.vm as unknown as { show: () => void }, "show")
          .mockImplementation(() => {
            order.push("show");
          });

        autoComplete.vm.$emit("option-select", { value: JOBS[0] });
        setTimeout(() => order.push("native-hide"), 0);
        await wrapper.vm.$nextTick();
        jest.runAllTimers();

        expect(order).toEqual(["native-hide", "show"]);
      } finally {
        jest.useRealTimers();
      }
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

    it("blocks multiple-selection edits and removals while readonly", async () => {
      const wrapper = await createMultipleWrapper([JOBS[0]], {
        readOnly: true,
      });

      await wrapper
        .find('[data-testid="selected-item-uuid-1"]')
        .trigger("click");
      await wrapper
        .findComponent(AutoComplete)
        .vm.$emit("option-select", { value: JOBS[1] });

      expect(wrapper.emitted("update:modelValue")).toBeUndefined();
      expect(wrapper.emitted("select")).toBeUndefined();
      expect(wrapper.emitted("unselect")).toBeUndefined();
    });
  });
});
