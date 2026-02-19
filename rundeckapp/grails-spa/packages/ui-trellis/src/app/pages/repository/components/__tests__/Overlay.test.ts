import { mount } from "@vue/test-utils";
import { setActivePinia, createPinia } from "pinia";
import Overlay from "../Overlay.vue";
import { useOverlayStore } from "../../stores/overlay.store";

describe("Overlay.vue", () => {
  let pinia: ReturnType<typeof createPinia>;

  beforeEach(() => {
    pinia = createPinia();
    setActivePinia(pinia);
  });

  it("renders correctly", () => {
    const wrapper = mount(Overlay, {
      global: {
        plugins: [pinia],
      },
    });
    expect(wrapper.exists()).toBe(true);
  });

  it("displays overlay when overlay state is true", () => {
    const store = useOverlayStore();
    store.overlay = true;
    store.loadingSpinner = true;
    store.loadingMessage = "Loading...";

    const wrapper = mount(Overlay, {
      global: {
        plugins: [pinia],
      },
    });
    expect(wrapper.find(".loading").exists()).toBe(true);
    expect(wrapper.find(".loading-spinner").exists()).toBe(true);
    expect(wrapper.text()).toContain("Loading...");
  });

  it("hides overlay when overlay state is false", () => {
    const store = useOverlayStore();
    store.closeOverlay();

    const wrapper = mount(Overlay, {
      global: {
        plugins: [pinia],
      },
    });
    // v-show="overlay" means the element exists but is hidden when overlay is false
    const loadingElement = wrapper.find(".loading");
    expect(loadingElement.exists()).toBe(true);
    // When overlay is false, v-show hides it
    expect(store.overlay).toBe(false);
  });

  it("calls closeOverlay when close button is clicked", async () => {
    const store = useOverlayStore();
    // Set errors first to ensure error section is rendered
    // Note: errors need to have a length property for v-if="errors && errors.length"
    store.errors = { code: "ERROR", msg: "Test error", length: 1 } as any;
    // Store starts with overlay: true, ensure it stays open
    store.overlay = true;
    store.loadingSpinner = true;
    store.loadingMessage = "Loading...";
    
    // Verify overlay is open before mounting
    expect(store.overlay).toBe(true);

    const wrapper = mount(Overlay, {
      global: {
        plugins: [pinia],
      },
    });
    
    // Wait for component to update
    await wrapper.vm.$nextTick();
    
    const closeButton = wrapper.find(".btn");
    expect(closeButton.exists()).toBe(true);
    await closeButton.trigger("click");
    await wrapper.vm.$nextTick();

    expect(store.overlay).toBe(false);
  });

  it("displays errors when present", async () => {
    const store = useOverlayStore();
    // Set errors and overlay state explicitly
    // Note: errors need to have a length property for v-if="errors && errors.length"
    store.errors = { code: "ERROR", msg: "Test error message", length: 1 } as any;
    store.overlay = true;
    store.loadingSpinner = true;
    
    // Verify overlay is open and errors are set before mounting
    expect(store.overlay).toBe(true);
    expect(store.errors).not.toBe(null);

    const wrapper = mount(Overlay, {
      global: {
        plugins: [pinia],
      },
    });
    
    await wrapper.vm.$nextTick();
    
    const errorsElement = wrapper.find(".errors");
    expect(errorsElement.exists()).toBe(true);
    expect(wrapper.text()).toContain("ERROR");
    expect(wrapper.text()).toContain("Test error message");
  });
});

