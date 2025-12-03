import { setActivePinia, createPinia } from "pinia";
import { useModalStore } from "../modal.store";

describe("ModalStore", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  describe("state", () => {
    it("should have initial state", () => {
      const store = useModalStore();
      expect(store.modalOpen).toBe(false);
    });
  });

  describe("actions", () => {
    describe("openModal", () => {
      it("should open modal when properties is truthy", () => {
        const store = useModalStore();
        store.modalOpen = false;

        store.openModal(true);

        expect(store.modalOpen).toBe(true);
      });

      it("should close modal when properties is falsy", () => {
        const store = useModalStore();
        store.modalOpen = true;

        store.openModal(false);

        expect(store.modalOpen).toBe(false);
      });
    });

    describe("closeModal", () => {
      it("should close modal and return a promise", async () => {
        const store = useModalStore();
        store.modalOpen = true;

        const result = await store.closeModal();

        expect(store.modalOpen).toBe(false);
        expect(result).toBeUndefined();
      });
    });
  });
});

