import { mount } from "@vue/test-utils";
import EditProjectFile from "./EditProjectFile.vue";
import * as editProjectFileService from "./editProjectFileService";

jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: () => ({
    rundeckClient: {},
  }),
  url: jest.fn().mockImplementation((path) => {
    return {
      href: `http://localhost:4440/${path}`,
    };
  }),
}));
jest.mock("./editProjectFileService", () => ({
  getFileText: jest.fn().mockResolvedValue({
    success: true,
    contents: "sample file content",
  }),
  saveProjectFile: jest.fn().mockResolvedValue({
    success: true,
    message: "File saved successfully.",
  }),
}));

jest.mock("../../../library/components/utils/AceEditor.vue", () => ({
  name: "AceEditor",
  functional: true,
  template: '<span class="ace_text ace_xml">sample file content</span>',
  methods: {
    getValue: jest.fn().mockReturnValue("sample file content"),
  },
}));

const mountEditProjectFile = async (props = {}) => {
  return mount(EditProjectFile, {
    props: {
      filename: "readme.md",
      project: "default",
      authAdmin: true,
      displayConfig: ["none"],
      ...props,
    },
    global: {
      mocks: {
        $t: (msg) => msg,
      },
    },
  });
};
describe("EditProjectFile", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });
  it.each([
    ["readme.md", "edit.readme.label"],
    ["motd.md", "edit.motd.label"],
  ])("renders the correct title for %s", async (filename, expectedTitle) => {
    const wrapper = await mountEditProjectFile({ filename });
    expect(wrapper.find('[data-test-id="title"]').text()).toContain(
      expectedTitle,
    );
  });
  it("renders file content inside AceEditor's span element when getFileText method returns successfully", async () => {
    const wrapper = await mountEditProjectFile();
    const aceEditor = wrapper.findComponent({ name: "AceEditor" });
    expect(aceEditor.exists()).toBe(true);
    const span = aceEditor.find("span.ace_text.ace_xml");
    expect(span.exists()).toBe(true);
    const spanHtml = span.text();
    expect(spanHtml).toContain("sample file content");
  });
  it("handles failure when getFileText method fails", async () => {
    const wrapper = await mountEditProjectFile();
    const notifyErrorSpy = jest.spyOn(wrapper.vm, "notifyError");
    (editProjectFileService.getFileText as jest.Mock).mockImplementationOnce(
      () => Promise.reject(new Error("Failed to fetch file")),
    );
    await wrapper.vm.getFileText();
    expect(notifyErrorSpy).toHaveBeenCalledWith("Failed to fetch file");
  });
  it("handles failure when user edits the file and fails to save it", async () => {
    const wrapper = await mountEditProjectFile();
    const notifyErrorSpy = jest.spyOn(wrapper.vm, "notifyError");
    (
      editProjectFileService.saveProjectFile as jest.Mock
    ).mockImplementationOnce(() =>
      Promise.reject(new Error("Failed to save file")),
    );
    wrapper.vm.fileText = "new content";
    await wrapper.find('[data-test-id="save"]').trigger("click");
    expect(notifyErrorSpy).toHaveBeenCalledWith("Failed to save file");
  });
  it("handles success when user edits the file and saves it", async () => {
    const wrapper = await mountEditProjectFile();
    wrapper.vm.fileText = "new content";
    await wrapper.find('[data-test-id="save"]').trigger("click");
    expect(editProjectFileService.saveProjectFile).toHaveBeenCalledWith(
      "default",
      "readme.md",
      "new content",
    );
  });
  it("displays warning message and configuration link when user is an admin and displayConfig is 'none'", async () => {
    const wrapper = await mountEditProjectFile();
    const footerText = wrapper.find(".card-footer").text();
    expect(footerText).toContain("file.warning.not.displayed.admin.message");
    expect(wrapper.find(".card-footer a").text()).toBe(
      "project.configuration.label",
    );
  });
  it("displays warning message and configuration link when user isn't an admin and displayConfig is 'none'", async () => {
    const wrapper = await mountEditProjectFile({ authAdmin: false });
    expect(
      wrapper.find('[data-test-id="nonadmin-warning-message"]').text(),
    ).toContain("file.warning.not.displayed.nonadmin.message");
  });
  it("navigates to the home page when the cancel button is clicked", async () => {
    const wrapper = await mountEditProjectFile();
    const button = wrapper.find('[data-test-id="cancel"]');

    await button.trigger("click");

    expect(window.location).toBe("http://localhost:4440/project/default/home");
  });
});
