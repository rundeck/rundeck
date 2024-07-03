import { mount, VueWrapper } from "@vue/test-utils";
import DetailsGroupModal from "../DetailsGroupModal.vue";

const createWrapper = async (
  propsData = {},
  extraOptions = {},
): Promise<VueWrapper<any>> => {
  return mount(DetailsGroupModal, {
    props: {
      modalId: "testModal",
      ...propsData,
    },
    global: {
      mocks: {
        $t: (msg: string) => msg,
      },
    },
    ...extraOptions,
  });
};

describe("DetailsGroupModal", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it("renders title correctly when title prop is provided", async () => {
    const wrapper = await createWrapper({ title: "Test Title" });
    const title = wrapper.find(".modal-title");
    expect(title.text()).toBe("Test Title");
  });

  it("renders title correctly when titleCode prop is provided", async () => {
    const wrapper = await createWrapper({ titleCode: "test.title.code" });
    const title = wrapper.find(".modal-title");
    expect(title.text()).toBe("test.title.code");
  });

  it("renders content slot correctly", async () => {
    const wrapper = await createWrapper(
      {},
      {
        slots: {
          default: "<p>Test Content</p>",
        },
      },
    );
    const content = wrapper.find(".modal-body p");
    expect(content.text()).toBe("Test Content");
  });

  it("renders content by default", async () => {
    const wrapper = await createWrapper({ content: "Default test Content" });
    const content = wrapper.find(".modal-body");
    expect(content.text()).toBe("Default test Content");
  });

  it("does not render close button when noCloseButton is true", async () => {
    const wrapper = await createWrapper({ noCloseButton: true });
    const closeButton = wrapper.find(".close");
    expect(closeButton.exists()).toBe(false);
  });

  it("does not render cancel button when noCancel is true", async () => {
    const wrapper = await createWrapper({ noCancel: true });
    const cancelButton = wrapper.find(".btn-default");
    expect(cancelButton.exists()).toBe(false);
  });

  it("renders custom buttons correctly and emit an event with button id that was clicked", async () => {
    const buttons = [
      {
        id: "button1",
        message: "Button 1",
        css: "btn-primary",
        js: jest.fn(),
      },
      {
        id: "button2",
        messageCode: "button.title",
        css: "btn-secondary",
        js: jest.fn(),
      },
    ];
    const wrapper = await createWrapper({ buttons });
    const buttonElements = wrapper.findAll("[data-test='extra-buttons']");
    expect(buttonElements.length).toBe(2);
    expect(buttonElements[0].text()).toBe("Button 1");
    expect(buttonElements[1].text()).toBe("button.title");

    await buttonElements[0].trigger("click");
    expect(wrapper.emitted().buttonClicked[0]).toEqual(["button1"]);
  });

  it("renders custom links correctly and emit an event with link object that was clicked", async () => {
    const links = [
      {
        id: "link1",
        href: "https://example.com",
        message: "Link 1",
        css: "btn-link",
        js: jest.fn(),
      },
      {
        id: "link2",
        href: "https://example2.com",
        message: "Link 2",
        css: "btn-link",
        js: jest.fn(),
      },
    ];
    const wrapper = await createWrapper({ links });
    const linkElements = wrapper.findAll("[data-test='extra-links']");
    expect(linkElements.length).toBe(2);
    expect(linkElements[0].text()).toBe("Link 1");
    expect(linkElements[1].text()).toBe("Link 2");
    await linkElements[0].trigger("click");
    expect(wrapper.emitted().linkClicked[0]).toEqual([links[0]]);
  });
});
