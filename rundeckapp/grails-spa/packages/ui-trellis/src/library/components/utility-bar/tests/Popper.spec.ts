import { mount } from "@vue/test-utils";
import Popper from "../Popper.vue";

jest.mock("@popperjs/core", () => ({
  createPopper: jest.fn(() => ({ destroy: jest.fn() })),
}));

interface MountOptions {
  slots?: Record<string, string>;
}

// Popper.vue reads this.$refs.wrapper.parentElement at mount time to know
// what counts as "inside" for the outside-click check, so the test mounts it
// into an explicit trigger element (as it's used in real usage, e.g. a
// button) rather than relying on Vue Test Utils' own attachTo container.
let triggerEl: HTMLElement | null = null;

const createWrapper = async (options: MountOptions = {}) => {
  triggerEl = document.createElement("button");
  document.body.appendChild(triggerEl);

  const wrapper = mount(Popper, {
    attachTo: triggerEl,
    slots: {
      default: '<div data-testid="slot-content">Popper body</div>',
      ...options.slots,
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
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
    triggerEl?.remove();
    triggerEl = null;
  });

  describe("rendering", () => {
    it("renders slot content", async () => {
      wrapper = await createWrapper();

      expect(findInDocument("slot-content")?.textContent).toBe("Popper body");
    });

    it("attaches its content to document.body", async () => {
      wrapper = await createWrapper();

      const popperContent = findInDocument("popper-content");
      expect(popperContent?.parentElement).toBe(document.body);
    });

    it("removes its content from document.body on unmount", async () => {
      wrapper = await createWrapper();
      wrapper.unmount();
      wrapper = null;

      expect(findInDocument("popper-content")).toBeNull();
    });
  });

  describe("close on outside click", () => {
    it("emits close when clicking outside the popper and its parent", async () => {
      wrapper = await createWrapper();

      const outside = document.createElement("div");
      document.body.appendChild(outside);
      outside.dispatchEvent(new MouseEvent("click", { bubbles: true }));
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted()).toHaveProperty("close");
      expect(wrapper.emitted("close")).toHaveLength(1);

      document.body.removeChild(outside);
    });

    it("does not emit close when clicking inside the popper content", async () => {
      wrapper = await createWrapper();

      findInDocument("slot-content")?.dispatchEvent(
        new MouseEvent("click", { bubbles: true }),
      );
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("close")).toBeUndefined();
    });
  });
});
