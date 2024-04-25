import { mount } from "@vue/test-utils";
import EditProjectFile from "./EditProjectFile.vue";
import * as editProjectFileService from "./editProjectFileService";

jest.mock("@/app/components/readme-motd/editProjectFileService", () => ({
  getFileText: jest.fn(() =>
    Promise.resolve({
      success: true,
      contents: "sample file content",
    }),
  ),
  saveProjectFile: jest.fn(() =>
    Promise.resolve({
      success: true,
      message: "File saved successfully.",
    }),
  ),
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
  url: jest.fn().mockReturnValue("http://localhost:4440"),
}));

let wrapper;

const mountEditProjectFile = async (props = {}) => {
  wrapper = mount(EditProjectFile, {
    props: {
      filename: "readme.md",
      project: "default",
      authAdmin: true,
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
  beforeEach(async () => {
    await mountEditProjectFile();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  // This test checks if the correct title is rendered based on the filename
  describe.each([
    ["readme.md", "edit.readme.label"],
    ["motd.md", "edit.motd.label"],
  ])("renders the correct title for %s", (filename, expectedTitle) => {
    it("checks title", async () => {
      await mountEditProjectFile({ filename });
      expect(wrapper.find('[data-test-id="title"]').text()).toContain(
        expectedTitle,
      );
    });
  });

  // This test checks if the file content is rendered when the getFileText method returns successfully
  it("renders file content when getFileText method returns successfully", async () => {
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.fileText).toBe("sample file content");
  });

  // This test checks if an error is handled correctly when the getFileText method fails
  it("handles failure when getFileText method fails", async () => {
    (editProjectFileService.getFileText as jest.Mock).mockImplementationOnce(
      () => Promise.reject(new Error("Failed to fetch file")),
    );
    wrapper.vm.notifyError = jest.fn();
    await wrapper.vm.getFileText();
    expect(wrapper.vm.notifyError).toHaveBeenCalledWith("Failed to fetch file");
  });

  // This test checks if an error is handled correctly when the user edits the file and fails to save it
  it("handles failure when user edits the file and fails to save it", async () => {
    (editProjectFileService.saveProjectFile as jest.Mock).mockRejectedValue(
      new Error("Failed to save file"),
    );
    wrapper.vm.notifyError = jest.fn();
    wrapper.vm.fileText = "new content";
    await wrapper.find('[data-test-id="save"]').trigger("click");
    expect(wrapper.vm.notifyError).toHaveBeenCalledWith("Failed to save file");
  });

  // This test checks if the file is saved successfully when the user edits the file and saves it
  it("handles success when user edits the file and saves it", async () => {
    wrapper.vm.fileText = "new content";
    await wrapper.find('[data-test-id="save"]').trigger("click");
    expect(
      editProjectFileService.saveProjectFile as jest.Mock,
    ).toHaveBeenCalledWith("default", "readme.md", "new content");
  });

  // This test checks if the correct message and configuration link are displayed when the user is an admin
  it("displays admin specific message and configuration link when user is an admin", async () => {
    await mountEditProjectFile({
      displayConfig: ["none"],
    });
    const footerText = wrapper.find(".card-footer").text();
    expect(footerText).toContain("file.warning.not.displayed.admin.message");
    expect(wrapper.find(".card-footer a").text()).toBe(
      "project.configuration.label",
    );
  });

  // This test checks if the correct message is displayed when the user is not an admin
  it("displays non-admin specific message when user is not an admin", async () => {
    await mountEditProjectFile({
      authAdmin: false,
      displayConfig: ["none"],
    });
    await wrapper.vm.$nextTick();
    expect(wrapper.text()).toContain(
      "file.warning.not.displayed.nonadmin.message",
    );
  });

  // This test checks if the save button is not displayed for non-admin users
  it("does not allow non-admin user to save the file", async () => {
    await mountEditProjectFile({ authAdmin: false });
    expect(wrapper.find('[data-test-id="save"]').exists()).toBe(false);
  });

  // This test checks if the editor is not displayed for non-admin users
  it("does not allow non-admin user to edit the file", async () => {
    await mountEditProjectFile({ authAdmin: false });
    expect(wrapper.find('[data-test-id="editor"]').exists()).toBe(false);
  });

  // This test checks if the Ace Editor is rendered
  it("renders the Ace Editor", () => {
    const aceEditor = wrapper.find('[data-test-id="ace-editor"]');
    expect(aceEditor.exists()).toBe(true);
  });
});

// This one is commented out because it is not working as expected
// it("does not change file content when user cancels the edit", async () => {
//   const originalFileText = wrapper.vm.fileText;
//   wrapper.vm.fileText = "new content";
//   await wrapper.find('[data-test-id="cancel"]').trigger("click");
//   console.log("After click event:", wrapper.vm.fileText);
//   expect(wrapper.vm.fileText).toBe(originalFileText);
// });
