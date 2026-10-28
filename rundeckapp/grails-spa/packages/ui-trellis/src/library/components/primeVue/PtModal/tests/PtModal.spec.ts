import { mount } from "@vue/test-utils";
import Dialog from "primevue/dialog";
import PtModal from "../PtModal.vue";
import type { PtModalButton } from "../ptModalTypes";

const BUTTONS: PtModalButton[] = [
  { id: "keep", label: "Keep editing", severity: "secondary", outlined: true },
  { id: "discard", label: "Discard", severity: "danger" },
];

const createWrapper = async (props = {}, slots = {}): Promise<any> => {
  const wrapper = mount(PtModal, {
    props: {
      modelValue: true,
      header: "Discard this step?",
      buttons: BUTTONS,
      ...props,
    },
    slots,
    global: { components: { Dialog } },
    attachTo: document.body,
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

const findInBody = (selector: string) => document.body.querySelector(selector);

describe("PtModal", () => {
  afterEach(() => {
    document.body.innerHTML = "";
  });

  describe("when it is open", () => {
    it("shows the header and the default slot content", async () => {
      await createWrapper({}, { default: "Your progress will be lost." });

      expect(findInBody(".p-dialog-title")?.textContent).toBe(
        "Discard this step?",
      );
      expect(findInBody(".p-dialog-content")?.textContent).toContain(
        "Your progress will be lost.",
      );
    });

    it("renders one button per entry in buttons", async () => {
      await createWrapper();

      expect(
        findInBody('[data-testid="pt-modal-button-keep"]')?.textContent,
      ).toContain("Keep editing");
      expect(
        findInBody('[data-testid="pt-modal-button-discard"]')?.textContent,
      ).toContain("Discard");
    });

    it("passes the button severity and variant to the button", async () => {
      await createWrapper();

      expect(
        findInBody('[data-testid="pt-modal-button-discard"]')?.classList,
      ).toContain("p-button-danger");
      expect(
        findInBody('[data-testid="pt-modal-button-keep"]')?.classList,
      ).toContain("p-button-outlined");
    });

    it("disables a button flagged as disabled", async () => {
      await createWrapper({
        buttons: [{ id: "go", label: "Go", disabled: true }],
      });

      expect(
        (findInBody('[data-testid="pt-modal-button-go"]') as HTMLButtonElement)
          .disabled,
      ).toBe(true);
    });
  });

  describe("when the user clicks a button", () => {
    it("emits button-click with the id of that button and stays open", async () => {
      const wrapper = await createWrapper();

      (
        findInBody('[data-testid="pt-modal-button-discard"]') as HTMLElement
      ).click();
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("button-click")).toEqual([["discard"]]);
      expect(wrapper.emitted("update:modelValue")).toBeUndefined();
    });
  });

  describe("when the user closes the modal", () => {
    it("emits update:modelValue false from the close button", async () => {
      const wrapper = await createWrapper();

      (findInBody(".p-dialog-close-button") as HTMLElement).click();
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("update:modelValue")![0]).toEqual([false]);
    });

    it("does not render the close button when closable is false", async () => {
      await createWrapper({ closable: false });

      expect(findInBody(".p-dialog-close-button")).toBeNull();
    });
  });

  describe("when it is closed", () => {
    it("renders nothing", async () => {
      await createWrapper({ modelValue: false });

      expect(findInBody(".pt-modal")).toBeNull();
    });
  });

  describe("slots", () => {
    it("replaces the generated buttons with the footer slot", async () => {
      await createWrapper(
        {},
        { footer: '<span data-testid="custom-footer">Custom</span>' },
      );

      expect(findInBody('[data-testid="custom-footer"]')).not.toBeNull();
      expect(findInBody('[data-testid="pt-modal-button-keep"]')).toBeNull();
    });

    it("replaces the header text with the header slot", async () => {
      await createWrapper(
        {},
        { header: '<span data-testid="custom-header">Custom title</span>' },
      );

      expect(findInBody('[data-testid="custom-header"]')).not.toBeNull();
    });

    it("renders no footer when there are no buttons and no footer slot", async () => {
      await createWrapper({ buttons: [] });

      expect(findInBody(".p-dialog-footer")).toBeNull();
    });
  });
});
