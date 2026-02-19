import { defineStore } from "pinia";
import { AxiosError } from "axios";

interface OverlayProperties {
  loadingSpinner?: boolean;
  loadingMessage?: string;
}

interface OverlayState {
  errors: AxiosError["response"] | null;
  rdBase: string | null;
  overlay: boolean;
  loadingMessage: string | null;
  loadingSpinner: boolean;
}

export const useOverlayStore = defineStore("overlay", {
  state: (): OverlayState => ({
    errors: null,
    rdBase: null,
    overlay: true,
    loadingMessage: null,
    loadingSpinner: false,
  }),
  actions: {
    closeOverlay(): void {
      this.overlay = false;
      this.loadingMessage = "";
      this.loadingSpinner = false;
    },
    openOverlay(properties: OverlayProperties | false | null): void {
      if (!properties) {
        this.overlay = false;
        this.loadingMessage = "";
        this.loadingSpinner = false;
      } else {
        this.overlay = true;
        if (properties.loadingMessage) {
          this.loadingMessage = properties.loadingMessage;
        }
        if (properties.loadingSpinner) {
          this.loadingSpinner = properties.loadingSpinner;
        }
      }
    },
  },
});

