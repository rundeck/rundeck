import { mount } from "@vue/test-utils";
import Button from "primevue/button";
import PtButton from "../PtButton.vue";

const createWrapper = async (props = {}) => {
  const wrapper = mount(PtButton, {
    props: { ...props },
    global: { components: { Button } },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("PtButton", () => {
  describe("label", () => {
    it("shows the label text inside the button so users know what the action does", async () => {
      const wrapper = await createWrapper({ label: "Save" });
      expect(wrapper.find('[data-testid="pt-button"]').text()).toBe("Save");
    });

    it("shows a different label when the label prop changes", async () => {
      const wrapper = await createWrapper({ label: "Cancel" });
      expect(wrapper.find('[data-testid="pt-button"]').text()).toBe("Cancel");
    });

    it("renders the button with no visible text when no label is provided", async () => {
      const wrapper = await createWrapper();
      expect(wrapper.find('[data-testid="pt-button"]').text()).toBe("");
    });
  });

  describe("loading state", () => {
    it("still shows the label text while the button is in a loading state", async () => {
      const wrapper = await createWrapper({ label: "Saving…", loading: true });
      expect(wrapper.find('[data-testid="pt-button"]').text()).toContain("Saving…");
    });
  });
});
