import { mount, flushPromises } from "@vue/test-utils";
import PluginSearch from "../PluginSearch.vue";
import PtInput from "@/library/components/primeVue/PtInput/PtInput.vue";

const createWrapper = async (options: { props?: Record<string, any> } = {}) => {
  const wrapper = mount(PluginSearch, {
    props: {
      ...options.props,
    },
    global: {
      stubs: { popover: true },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("PluginSearch", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("non-EA mode (default)", () => {
    it("shows the filter label", async () => {
      const wrapper = await createWrapper();

      expect(
        wrapper.find('[data-testid="plugin-search-label"]').exists(),
      ).toBe(true);
    });

    it("shows the search input", async () => {
      const wrapper = await createWrapper();

      expect(
        wrapper.find('[data-testid="plugin-search-input"]').exists(),
      ).toBe(true);
    });

    it("shows the search button", async () => {
      const wrapper = await createWrapper();

      expect(
        wrapper.find('[data-testid="plugin-search-button"]').exists(),
      ).toBe(true);
    });

    it("emits search event with typed filter when search button is clicked", async () => {
      const wrapper = await createWrapper();

      await wrapper
        .find('[data-testid="plugin-search-input"]')
        .setValue("my-filter");
      await wrapper
        .find('[data-testid="plugin-search-button"]')
        .trigger("click");

      expect(wrapper.emitted("search")).toHaveLength(1);
      expect(wrapper.emitted("search")[0]).toEqual(["my-filter"]);
    });

    it("emits search event with typed filter when Enter is pressed", async () => {
      const wrapper = await createWrapper();

      await wrapper
        .find('[data-testid="plugin-search-input"]')
        .setValue("enter-filter");
      await wrapper
        .find('[data-testid="plugin-search-input"]')
        .trigger("keydown.enter");

      expect(wrapper.emitted("search")).toHaveLength(1);
      expect(wrapper.emitted("search")[0]).toEqual(["enter-filter"]);
    });

    it("emits search with empty string when no value entered", async () => {
      const wrapper = await createWrapper();

      await wrapper
        .find('[data-testid="plugin-search-button"]')
        .trigger("click");

      expect(wrapper.emitted("search")).toHaveLength(1);
      expect(wrapper.emitted("search")[0]).toEqual([""]);
    });

    it("does not emit searching event when typing", async () => {
      const wrapper = await createWrapper();

      await wrapper
        .find('[data-testid="plugin-search-input"]')
        .setValue("text");

      expect(wrapper.emitted("searching")).toBeFalsy();
    });
  });

  describe("EA mode (ea=true)", () => {
    it("hides the filter label", async () => {
      const wrapper = await createWrapper({ props: { ea: true } });

      expect(
        wrapper.find('[data-testid="plugin-search-label"]').exists(),
      ).toBe(false);
    });

    it("hides the search button", async () => {
      const wrapper = await createWrapper({ props: { ea: true } });

      expect(
        wrapper.find('[data-testid="plugin-search-button"]').exists(),
      ).toBe(false);
    });

    it("renders the PtInput component", async () => {
      const wrapper = await createWrapper({ props: { ea: true } });

      expect(wrapper.findComponent(PtInput).exists()).toBe(true);
    });

    it("emits searching=true immediately when typing", async () => {
      jest.useFakeTimers();
      const wrapper = await createWrapper({ props: { ea: true } });

      const ptInput = wrapper.findComponent(PtInput);
      await ptInput.vm.$emit("update:modelValue", "search text");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("searching")).toBeTruthy();
      expect(wrapper.emitted("searching")[0]).toEqual([true]);

      jest.useRealTimers();
    });

    it("emits search event with the typed value after debounce", async () => {
      jest.useFakeTimers();
      const wrapper = await createWrapper({ props: { ea: true } });

      const ptInput = wrapper.findComponent(PtInput);
      await ptInput.vm.$emit("update:modelValue", "debounced-filter");
      await wrapper.vm.$nextTick();

      jest.advanceTimersByTime(300);
      await flushPromises();
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("search")).toHaveLength(1);
      expect(wrapper.emitted("search")[0]).toEqual(["debounced-filter"]);

      jest.useRealTimers();
    });

    it("emits searching=false after the debounce timer fires", async () => {
      jest.useFakeTimers();
      const wrapper = await createWrapper({ props: { ea: true } });

      const ptInput = wrapper.findComponent(PtInput);
      await ptInput.vm.$emit("update:modelValue", "text");
      await wrapper.vm.$nextTick();

      jest.advanceTimersByTime(300);
      await flushPromises();
      await wrapper.vm.$nextTick();

      const searchingEmits = wrapper.emitted("searching") as any[][];
      expect(searchingEmits[searchingEmits.length - 1]).toEqual([false]);

      jest.useRealTimers();
    });

    it("resets debounce timer when typing occurs before it fires", async () => {
      jest.useFakeTimers();
      const wrapper = await createWrapper({ props: { ea: true } });

      const ptInput = wrapper.findComponent(PtInput);
      await ptInput.vm.$emit("update:modelValue", "fir");
      await wrapper.vm.$nextTick();

      jest.advanceTimersByTime(100);

      await ptInput.vm.$emit("update:modelValue", "first");
      await wrapper.vm.$nextTick();

      jest.advanceTimersByTime(300);
      await flushPromises();
      await wrapper.vm.$nextTick();

      // Only one search emitted for the final value
      expect(wrapper.emitted("search")).toHaveLength(1);
      expect(wrapper.emitted("search")[0]).toEqual(["first"]);

      jest.useRealTimers();
    });
  });
});
