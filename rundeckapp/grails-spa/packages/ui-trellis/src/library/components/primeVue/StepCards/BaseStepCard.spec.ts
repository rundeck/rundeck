import { mount } from "@vue/test-utils";
import BaseStepCard from "./BaseStepCard.vue";

const mockPluginDetails = {
  title: "Test Step",
  description: "A test step",
};

const mockConfig = {
  id: "step-1",
  description: "Step description",
};

describe("BaseStepCard", () => {
  it("renders the component", () => {
    const wrapper = mount(BaseStepCard, {
      props: {
        pluginDetails: mockPluginDetails,
        config: mockConfig,
      },
      slots: {
        content: "<div data-testid='slot-content'>Content</div>",
      },
    });
    expect(wrapper.exists()).toBe(true);
  });

  it("renders StepCardHeader when header slot is not provided", () => {
    const wrapper = mount(BaseStepCard, {
      props: {
        pluginDetails: mockPluginDetails,
        config: mockConfig,
      },
      slots: {
        content: "<div>Content</div>",
      },
    });
    expect(wrapper.findComponent({ name: "StepCardHeader" }).exists()).toBe(true);
  });

  it("renders custom header when header slot is provided", () => {
    const wrapper = mount(BaseStepCard, {
      props: {
        pluginDetails: mockPluginDetails,
        config: mockConfig,
      },
      slots: {
        header: "<div data-testid='custom-header'>Custom Header</div>",
        content: "<div>Content</div>",
      },
    });
    expect(wrapper.findComponent({ name: "StepCardHeader" }).exists()).toBe(false);
    expect(wrapper.find("[data-testid='custom-header']").exists()).toBe(true);
    expect(wrapper.find("[data-testid='custom-header']").text()).toBe("Custom Header");
  });

  it("renders content slot", () => {
    const wrapper = mount(BaseStepCard, {
      props: {
        pluginDetails: mockPluginDetails,
        config: mockConfig,
      },
      slots: {
        content: "<div data-testid='slot-content'>Slot content</div>",
      },
    });
    expect(wrapper.find("[data-testid='slot-content']").exists()).toBe(true);
    expect(wrapper.find("[data-testid='slot-content']").text()).toBe("Slot content");
  });

  it("renders footer slot when provided", () => {
    const wrapper = mount(BaseStepCard, {
      props: {
        pluginDetails: mockPluginDetails,
        config: mockConfig,
      },
      slots: {
        content: "<div>Content</div>",
        footer: "<div data-testid='slot-footer'>Footer</div>",
      },
    });
    expect(wrapper.find("[data-testid='slot-footer']").exists()).toBe(true);
    expect(wrapper.find("[data-testid='slot-footer']").text()).toBe("Footer");
  });

  it("emits delete when StepCardHeader delete is clicked", async () => {
    const wrapper = mount(BaseStepCard, {
      props: {
        pluginDetails: mockPluginDetails,
        config: mockConfig,
      },
      slots: {
        content: "<div>Content</div>",
      },
    });
    const header = wrapper.findComponent({ name: "StepCardHeader" });
    await header.vm.$emit("delete");
    expect(wrapper.emitted("delete")).toBeTruthy();
  });

  it("emits duplicate when StepCardHeader duplicate is clicked", async () => {
    const wrapper = mount(BaseStepCard, {
      props: {
        pluginDetails: mockPluginDetails,
        config: mockConfig,
      },
      slots: {
        content: "<div>Content</div>",
      },
    });
    const header = wrapper.findComponent({ name: "StepCardHeader" });
    await header.vm.$emit("duplicate");
    expect(wrapper.emitted("duplicate")).toBeTruthy();
  });

  it("emits edit when StepCardHeader edit is clicked", async () => {
    const wrapper = mount(BaseStepCard, {
      props: {
        pluginDetails: mockPluginDetails,
        config: mockConfig,
      },
      slots: {
        content: "<div>Content</div>",
      },
    });
    const header = wrapper.findComponent({ name: "StepCardHeader" });
    await header.vm.$emit("edit");
    expect(wrapper.emitted("edit")).toBeTruthy();
  });

  it("toggles collapsed state when StepCardHeader toggle is clicked", async () => {
    const wrapper = mount(BaseStepCard, {
      props: {
        pluginDetails: mockPluginDetails,
        config: mockConfig,
      },
      slots: {
        content: "<div>Content</div>",
      },
    });
    const card = wrapper.find(".baseStepCard");
    expect(card.classes()).not.toContain("collapsed");

    const header = wrapper.findComponent({ name: "StepCardHeader" });
    await header.vm.$emit("toggle");
    await wrapper.vm.$nextTick();

    expect(card.classes()).toContain("collapsed");
  });

  it("uses expanded prop when provided", () => {
    const wrapper = mount(BaseStepCard, {
      props: {
        pluginDetails: mockPluginDetails,
        config: mockConfig,
        expanded: false,
      },
      slots: {
        content: "<div>Content</div>",
      },
    });
    const card = wrapper.find(".baseStepCard");
    expect(card.classes()).toContain("collapsed");
  });

  it("applies cardClass prop to the card", () => {
    const wrapper = mount(BaseStepCard, {
      props: {
        pluginDetails: mockPluginDetails,
        config: mockConfig,
        cardClass: "custom-class",
      },
      slots: {
        content: "<div>Content</div>",
      },
    });
    const card = wrapper.find(".baseStepCard");
    expect(card.classes()).toContain("custom-class");
  });
});
