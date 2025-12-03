import { setActivePinia, createPinia } from "pinia";
import { useOverlayStore } from "../overlay.store";

describe("OverlayStore", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  describe("state", () => {
    it("should have initial state", () => {
      const store = useOverlayStore();
      expect(store.errors).toBe(null);
      expect(store.rdBase).toBe(null);
      expect(store.overlay).toBe(true);
      expect(store.loadingMessage).toBe(null);
      expect(store.loadingSpinner).toBe(false);
    });
  });

  describe("actions", () => {
    describe("closeOverlay", () => {
      it("should close overlay and reset state", () => {
        const store = useOverlayStore();
        store.overlay = true;
        store.loadingMessage = "Loading...";
        store.loadingSpinner = true;

        store.closeOverlay();

        expect(store.overlay).toBe(false);
        expect(store.loadingMessage).toBe("");
        expect(store.loadingSpinner).toBe(false);
      });
    });

    describe("openOverlay", () => {
      it("should open overlay with properties", () => {
        const store = useOverlayStore();
        store.overlay = false;

        store.openOverlay({
          loadingSpinner: true,
          loadingMessage: "Loading plugins",
        });

        expect(store.overlay).toBe(true);
        expect(store.loadingSpinner).toBe(true);
        expect(store.loadingMessage).toBe("Loading plugins");
      });

      it("should close overlay when properties is false", () => {
        const store = useOverlayStore();
        store.overlay = true;
        store.loadingMessage = "Loading...";
        store.loadingSpinner = true;

        store.openOverlay(false);

        expect(store.overlay).toBe(false);
        expect(store.loadingMessage).toBe("");
        expect(store.loadingSpinner).toBe(false);
      });

      it("should handle partial properties", () => {
        const store = useOverlayStore();
        store.overlay = false;

        store.openOverlay({
          loadingSpinner: true,
        });

        expect(store.overlay).toBe(true);
        expect(store.loadingSpinner).toBe(true);
        expect(store.loadingMessage).toBe(null);
      });
    });
  });
});

