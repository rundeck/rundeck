import { mount } from "@vue/test-utils";
import PluginIcon from "../PluginIcon.vue";

const createWrapper = async (options: { props?: Record<string, any> } = {}) => {
  const wrapper = mount(PluginIcon, {
    props: {
      detail: {},
      ...options.props,
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("PluginIcon", () => {
  describe("icon URL display", () => {
    it("renders an image when detail.iconUrl is provided", async () => {
      const wrapper = await createWrapper({
        props: { detail: { iconUrl: "https://example.com/icon.png" } },
      });

      expect(wrapper.find('[data-testid="plugin-icon-image"]').exists()).toBe(
        true,
      );
    });

    it("sets the correct src on the image from detail.iconUrl", async () => {
      const wrapper = await createWrapper({
        props: { detail: { iconUrl: "https://example.com/icon.png" } },
      });

      expect(
        wrapper.find('[data-testid="plugin-icon-image"]').attributes("src"),
      ).toBe("https://example.com/icon.png");
    });

    it("does not render glyphicon, faicon, fabicon or default icon when iconUrl is provided", async () => {
      const wrapper = await createWrapper({
        props: {
          detail: {
            iconUrl: "https://example.com/icon.png",
            providerMetadata: { glyphicon: "cog", faicon: "star", fabicon: "github" },
          },
        },
      });

      expect(
        wrapper.find('[data-testid="plugin-icon-glyphicon"]').exists(),
      ).toBe(false);
      expect(wrapper.find('[data-testid="plugin-icon-faicon"]').exists()).toBe(
        false,
      );
      expect(wrapper.find('[data-testid="plugin-icon-fabicon"]').exists()).toBe(
        false,
      );
      expect(wrapper.find('[data-testid="plugin-icon-default"]').exists()).toBe(
        false,
      );
    });
  });

  describe("glyphicon display", () => {
    it("renders a glyphicon when providerMetadata.glyphicon is set", async () => {
      const wrapper = await createWrapper({
        props: { detail: { providerMetadata: { glyphicon: "cog" } } },
      });

      expect(
        wrapper.find('[data-testid="plugin-icon-glyphicon"]').exists(),
      ).toBe(true);
    });

    it("applies the correct glyphicon class", async () => {
      const wrapper = await createWrapper({
        props: { detail: { providerMetadata: { glyphicon: "cog" } } },
      });

      expect(
        wrapper
          .find('[data-testid="plugin-icon-glyphicon"]')
          .classes(),
      ).toContain("glyphicon-cog");
    });

    it("does not render image or faicon when glyphicon is set", async () => {
      const wrapper = await createWrapper({
        props: { detail: { providerMetadata: { glyphicon: "cog" } } },
      });

      expect(wrapper.find('[data-testid="plugin-icon-image"]').exists()).toBe(
        false,
      );
      expect(wrapper.find('[data-testid="plugin-icon-faicon"]').exists()).toBe(
        false,
      );
    });
  });

  describe("faicon display", () => {
    it("renders a faicon when providerMetadata.faicon is set", async () => {
      const wrapper = await createWrapper({
        props: { detail: { providerMetadata: { faicon: "star" } } },
      });

      expect(wrapper.find('[data-testid="plugin-icon-faicon"]').exists()).toBe(
        true,
      );
    });

    it("applies the correct faicon class", async () => {
      const wrapper = await createWrapper({
        props: { detail: { providerMetadata: { faicon: "star" } } },
      });

      expect(
        wrapper.find('[data-testid="plugin-icon-faicon"]').classes(),
      ).toContain("fa-star");
    });

    it("does not render glyphicon when faicon is set", async () => {
      const wrapper = await createWrapper({
        props: { detail: { providerMetadata: { faicon: "star" } } },
      });

      expect(
        wrapper.find('[data-testid="plugin-icon-glyphicon"]').exists(),
      ).toBe(false);
    });
  });

  describe("fabicon display", () => {
    it("renders a fabicon when providerMetadata.fabicon is set", async () => {
      const wrapper = await createWrapper({
        props: { detail: { providerMetadata: { fabicon: "github" } } },
      });

      expect(
        wrapper.find('[data-testid="plugin-icon-fabicon"]').exists(),
      ).toBe(true);
    });

    it("applies the correct fabicon class", async () => {
      const wrapper = await createWrapper({
        props: { detail: { providerMetadata: { fabicon: "github" } } },
      });

      expect(
        wrapper.find('[data-testid="plugin-icon-fabicon"]').classes(),
      ).toContain("fa-github");
    });
  });

  describe("default icon display", () => {
    it("renders the default rdicon when no icon type is set", async () => {
      const wrapper = await createWrapper({
        props: { detail: {} },
      });

      expect(wrapper.find('[data-testid="plugin-icon-default"]').exists()).toBe(
        true,
      );
    });

    it("renders the default rdicon when providerMetadata is empty", async () => {
      const wrapper = await createWrapper({
        props: { detail: { providerMetadata: {} } },
      });

      expect(wrapper.find('[data-testid="plugin-icon-default"]').exists()).toBe(
        true,
      );
    });

    it("does not render image, glyphicon, faicon or fabicon when no icon is set", async () => {
      const wrapper = await createWrapper({
        props: { detail: {} },
      });

      expect(wrapper.find('[data-testid="plugin-icon-image"]').exists()).toBe(
        false,
      );
      expect(
        wrapper.find('[data-testid="plugin-icon-glyphicon"]').exists(),
      ).toBe(false);
      expect(wrapper.find('[data-testid="plugin-icon-faicon"]').exists()).toBe(
        false,
      );
      expect(wrapper.find('[data-testid="plugin-icon-fabicon"]').exists()).toBe(
        false,
      );
    });
  });

  describe("wrapper class", () => {
    it("applies the default plugin-icon-wrapper class to the wrapper span", async () => {
      const wrapper = await createWrapper({
        props: { detail: {} },
      });

      expect(wrapper.classes()).toContain("plugin-icon-wrapper");
    });

    it("applies a custom iconClass to the wrapper span", async () => {
      const wrapper = await createWrapper({
        props: { detail: {}, iconClass: "my-custom-class" },
      });

      expect(wrapper.classes()).toContain("my-custom-class");
    });

    it("does not apply default class when a custom iconClass is provided", async () => {
      const wrapper = await createWrapper({
        props: { detail: {}, iconClass: "my-custom-class" },
      });

      expect(wrapper.classes()).not.toContain("plugin-icon-wrapper");
    });
  });
});
