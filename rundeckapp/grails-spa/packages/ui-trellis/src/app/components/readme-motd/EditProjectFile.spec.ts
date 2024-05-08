import { mount } from "@vue/test-utils";
import EditProjectFile from "./EditProjectFile.vue";
import * as editProjectFileService from "./editProjectFileService";

jest.mock("@/app/components/readme-motd/editProjectFileService", () => ({
  getFileText: jest.fn().mockResolvedValue({
    success: true,
    contents: "sample file content",
  }),
  saveProjectFile: jest.fn().mockResolvedValue({
    success: true,
    message: "File saved successfully.",
  }),
}));

jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    rundeckClient: {
      getFileText: jest.fn().mockResolvedValue({
        filename: "readme.md",
        success: true,
        contents: "Some content",
        rdBase: "http://localhost:4440",
      }),
      saveProjectFile: jest.fn().mockResolvedValue({
        success: true,
        message: "File saved successfully.",
      }),
    },
    rdBase: "http://localhost:4440",
  }),
  url: jest.fn().mockReturnValue({
    href: "http://localhost:4440/project/default/home",
  }),
}));

jest.mock("../../../library/components/utils/AceEditor.vue", () => ({
  name: "AceEditor",
  functional: true,
  template: '<span class="ace_text ace_xml"></span>',
  methods: {
    getValue: jest.fn().mockReturnValue("sample file content"),
  },
}));

describe("EditProjectFile", () => {
  let wrapper;
  let originalLocation;

  const mountEditProjectFile = async (props = {}) => {
    wrapper = mount(EditProjectFile, {
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
    await wrapper.vm.$nextTick();
  };

  beforeEach(async () => {
    originalLocation = window.location;
    delete (window as any).location;
    window.location = {
      ...originalLocation,
      assign: jest.fn(),
      href: jest.fn(),
      replace: jest.fn(),
      toString: () => (window.location as any)._href || "http://localhost:4440",
    };

    await mountEditProjectFile();
  });

  afterEach(() => {
    jest.clearAllMocks();
    window.location = originalLocation;
  });

  it.each([
    ["readme.md", "edit.readme.label"],
    ["motd.md", "edit.motd.label"],
  ])("renders the correct title for %s", async (filename, expectedTitle) => {
    await mountEditProjectFile({ filename });
    expect(wrapper.find('[data-test-id="title"]').text()).toContain(
      expectedTitle
    );
  });

  it("renders file content inside AceEditor's span element when getFileText method returns successfully", async () => {
    wrapper.vm.getFileText = jest.fn().mockResolvedValue("sample file content");
    await wrapper.vm.getFileText();
    await wrapper.vm.$nextTick();
    const aceEditor = wrapper.findComponent({ name: "AceEditor" });
    expect(aceEditor.exists()).toBe(true);
    const span = aceEditor.find("span.ace_text.ace_xml");
    expect(span.exists()).toBe(true);
    const spanHtml = span.html();
    expect(spanHtml).toContain("sample file content");
  });
  it("handles failure when getFileText method fails", async () => {
    (editProjectFileService.getFileText as jest.Mock).mockImplementationOnce(
      () => Promise.reject(new Error("Failed to fetch file"))
    );
    jest.spyOn(wrapper.vm, "notifyError");
    await wrapper.vm.getFileText();
    expect(wrapper.vm.notifyError).toHaveBeenCalledWith("Failed to fetch file");
  });

  it("handles failure when user edits the file and fails to save it", async () => {
    (editProjectFileService.saveProjectFile as jest.Mock).mockRejectedValue(
      new Error("Failed to save file")
    );
    jest.spyOn(wrapper.vm, "notifyError");
    wrapper.vm.fileText = "new content";
    await wrapper.find('[data-test-id="save"]').trigger("click");
    expect(wrapper.vm.notifyError).toHaveBeenCalledWith("Failed to save file");
  });
  it("handles success when user edits the file and saves it", async () => {
    wrapper.vm.fileText = "new content";
    await wrapper.find('[data-test-id="save"]').trigger("click");
    expect(
      editProjectFileService.saveProjectFile as jest.Mock
    ).toHaveBeenCalledWith("default", "readme.md", "new content");
  });

  it("displays warning message and configuration link when user is an admin and displayConfig value is 'none", async () => {
    const footerText = wrapper.find(".card-footer").text();
    expect(footerText).toContain("file.warning.not.displayed.admin.message");
    expect(wrapper.find(".card-footer a").text()).toBe(
      "project.configuration.label"
    );
  });

  it("displays warning message and configuration link when user isn't an admin and displayConfig value is 'none", async () => {
    await mountEditProjectFile({
      authAdmin: false,
    });
    expect(
      wrapper.find('[data-test-id="nonadmin-warning-message"]').text()
    ).toContain("file.warning.not.displayed.nonadmin.message");
  });
  it("navigates to the home page when the cancel button is clicked", async () => {
    await mountEditProjectFile();
    const button = wrapper.find('[data-test-id="cancel"]');

    await button.trigger("click");

    expect(window.location).toBe("http://localhost:4440/project/default/home");
  });
});
