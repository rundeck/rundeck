import { mount } from "@vue/test-utils";
import EditProjectFile from "./EditProjectFile.vue";
import * as editProjectFileService from "./editProjectFileService";
import AceEditor from "../../../library/components/utils/AceEditor.vue";

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
  url: jest.fn().mockReturnValue("http://localhost:4440"),
}));
jest.mock("../../../library/components/utils/AceEditor.vue", () => ({
  render: () => {},
  methods: {
    getValue: jest.fn().mockReturnValue("sample file content"),
  },
}));
let originalLocation;

beforeAll(() => {
  originalLocation = window.location;
  delete (window as any).location;
  window.location = {
    ...originalLocation,
    assign: jest.fn(),
    href: jest.fn(),
    replace: jest.fn(),

    toString: () => (window.location as any)._href || "http://localhost:4440",
  };
});

afterAll(() => {
  window.location = originalLocation;
});

let wrapper;
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
      stubs: {
        "ace-editor": AceEditor,
      },
    },
  });
  await wrapper.vm.$nextTick();
};

describe("EditProjectFile", () => {
  beforeEach(async () => {
    await mountEditProjectFile();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });
  it.each([
    ["readme.md", "edit.readme.label"],
    ["motd.md", "edit.motd.label"],
  ])("renders the correct title for %s", async (filename, expectedTitle) => {
    await mountEditProjectFile({ filename });
    expect(wrapper.find('[data-test-id="title"]').text()).toContain(
      expectedTitle,
    );
  });
  // it("renders file content when getFileText method returns successfully", async () => {
  //   wrapper.vm.getFileText = jest.fn().mockResolvedValue("sample file content");
  //   await wrapper.vm.getFileText();

  //   await wrapper.vm.$nextTick();

  //   const aceEditor = wrapper.findComponent({ ref: "aceEditor" });
  //   expect(aceEditor.exists()).toBe(true);
  //   if (aceEditor.exists()) {
  //     await wrapper.vm.$nextTick();

  //     const span = aceEditor.find(".ace_text.ace_xml");
  //     if (span.exists()) {
  //       expect(span.text()).toBe("sample file content");
  //     }
  //   }
  // });
  it("renders file content when getFileText method returns successfully", async () => {
    const expectedContent = "sample file content";
    wrapper.vm.getFileText = jest.fn().mockResolvedValue(expectedContent);
    await wrapper.vm.getFileText();

    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    const aceEditor = wrapper.findComponent({ ref: "aceEditor" });
    expect(aceEditor.exists()).toBe(true);

    const editorContent = aceEditor.vm.getValue();
    expect(editorContent).toBe(expectedContent);
  });

  it("handles failure when getFileText method fails", async () => {
    (editProjectFileService.getFileText as jest.Mock).mockImplementationOnce(
      () => Promise.reject(new Error("Failed to fetch file")),
    );
    jest.spyOn(wrapper.vm, "notifyError");
    await wrapper.vm.getFileText();
    expect(wrapper.vm.notifyError).toHaveBeenCalledWith("Failed to fetch file");
  });

  it("handles failure when user edits the file and fails to save it", async () => {
    (editProjectFileService.saveProjectFile as jest.Mock).mockRejectedValue(
      new Error("Failed to save file"),
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
      editProjectFileService.saveProjectFile as jest.Mock,
    ).toHaveBeenCalledWith("default", "readme.md", "new content");
  });

  it("displays warning message and configuration link when user is an admin and displayConfig value is 'none", async () => {
    const footerText = wrapper.find(".card-footer").text();
    expect(footerText).toContain("file.warning.not.displayed.admin.message");
    expect(wrapper.find(".card-footer a").text()).toBe(
      "project.configuration.label",
    );
  });

  it("displays warning message and configuration link when user isn't an admin and displayConfig value is 'none", async () => {
    await mountEditProjectFile({
      authAdmin: false,
    });
    expect(
      wrapper.find('[data-test-id="nonadmin-warning-message"]').text(),
    ).toContain("file.warning.not.displayed.nonadmin.message");
  });

  it("navigates to the home page when the cancel button is clicked", async () => {
    await mountEditProjectFile();
    const expectedUrl = `http://localhost:4440/project/${wrapper.vm.project}/home`;

    await wrapper.find('[data-test-id="cancel"]').trigger("click");
    await wrapper.vm.$nextTick();
    expect(window.location.href).toBe(expectedUrl);
  });
});
