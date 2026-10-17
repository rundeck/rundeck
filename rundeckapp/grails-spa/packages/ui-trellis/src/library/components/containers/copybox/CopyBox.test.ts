import { vi } from "vitest";
import { mount } from "@vue/test-utils";
import CopyBox from "./CopyBox.vue";
import { CopyToClipboard } from "../../../utilities/Clipboard";
vi.mock("../../../utilities/Clipboard");

const mockedCopyToClipboard = CopyToClipboard as vi.MockedFunction<
  typeof CopyToClipboard
>;

describe("CopyBox", () => {
  beforeEach(() => {
    console.error = vi.fn();
  });

  afterEach(() => vi.clearAllMocks());

  test("should copy text to clipboard when clicked", async () => {
    mockedCopyToClipboard.mockResolvedValueOnce(true);
    const wrapper = mount(CopyBox, {
      props: { content: "text" },
    });

    // Simulate a click event on the component
    await wrapper.trigger("click");
    await wrapper.vm.$nextTick();

    expect(wrapper.text()).toContain("Copied to clipboard!");
  });

  test("should display error message after failed copy", async () => {
    mockedCopyToClipboard.mockRejectedValueOnce(false);
    const wrapper = mount(CopyBox, { props: { content: "text" } });

    // Simulate a click event on the component
    await wrapper.trigger("click");
    await wrapper.vm.$nextTick();

    // Verify that the error message is displayed
    expect(wrapper.text()).toContain("Copy failed!");
  });
});
