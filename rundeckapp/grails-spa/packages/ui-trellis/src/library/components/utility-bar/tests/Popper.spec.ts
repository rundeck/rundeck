import { mount } from "@vue/test-utils";
import Popper from "../Popper.vue";

jest.mock("@popperjs/core", () => ({
  createPopper: jest.fn(() => ({ destroy: jest.fn() })),
}));

interface MountOptions {
  slots?: Record<string, string>;
}

const createWrapper = (options: MountOptions = {}) => {
  return mount(Popper, {
    attachTo: document.body,
    slots: {
      default: '<div data-testid="slot-content">Popper body</div>',
      ...options.slots,
    },
  });
};

// Popper.vue moves its content node onto document.body for positioning, so it
// is no longer a descendant of wrapper.element after mount - query the
// document directly for it, per data-testid selector convention.
const findInDocument = (testId: string) =>
  document.querySelector(`[data-testid="${testId}"]`);

describe("Popper", () => {
  let wrapper: ReturnType<typeof mount> | null = null;

  afterEach(() => {
    wrapper?.unmount();
    wrapper = null;
  });

  describe("rendering", () => {
    it("renders slot content", () => {
      wrapper = createWrapper();

      expect(findInDocument("slot-content")?.textContent).toBe("Popper body");
    });

    it("attaches its content to document.body", () => {
      wrapper = createWrapper();

      const popperContent = findInDocument("popper-content");
      expect(popperContent?.parentElement).toBe(document.body);
    });

    it("removes its content from document.body on unmount", () => {
      wrapper = createWrapper();
      wrapper.unmount();
      wrapper = null;

      expect(findInDocument("popper-content")).toBeNull();
    });
  });

  describe("close on outside click", () => {
    it("emits close when clicking outside the popper and its parent", async () => {
      wrapper = createWrapper();

      const outside = document.createElement("div");
      document.body.appendChild(outside);
      outside.dispatchEvent(new MouseEvent("click", { bubbles: true }));
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted()).toHaveProperty("close");
      expect(wrapper.emitted("close")).toHaveLength(1);

      document.body.removeChild(outside);
    });

    it("does not emit close when clicking inside the popper content", async () => {
      wrapper = createWrapper();

      findInDocument("slot-content")?.dispatchEvent(
        new MouseEvent("click", { bubbles: true }),
      );
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("close")).toBeUndefined();
    });
  });
});
