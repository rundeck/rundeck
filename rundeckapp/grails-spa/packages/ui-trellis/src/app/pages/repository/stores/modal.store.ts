import { defineStore } from "pinia";

interface ModalState {
  modalOpen: boolean;
}

export const useModalStore = defineStore("modal", {
  state: (): ModalState => ({
    modalOpen: false,
  }),
  actions: {
    closeModal(): Promise<void> {
      return new Promise((resolve) => {
        this.modalOpen = false;
        resolve();
      });
    },
    openModal(properties: boolean | null | undefined): void {
      if (!properties) {
        this.modalOpen = false;
      } else {
        this.modalOpen = true;
      }
    },
  },
});

