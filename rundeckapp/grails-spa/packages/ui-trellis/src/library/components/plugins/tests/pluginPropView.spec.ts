import { mount, flushPromises } from "@vue/test-utils";
import PluginPropView from "../pluginPropView.vue";
import * as Clipboard from "../../../utilities/Clipboard";

jest.mock("../../../utilities/Clipboard");
const mockedClipboard = Clipboard as jest.Mocked<typeof Clipboard>;

interface MountOptions {
  props?: Record<string, any>;
  stubs?: Record<string, boolean | object>;
}

let originalMutationObserver: typeof MutationObserver;

const createWrapper = async (options: MountOptions = {}) => {
  const wrapper = mount(PluginPropView, {
    props: {
      prop: { type: "String", title: "My Prop", desc: "Prop description" },
      value: "hello",
      allowCopy: false,
      ...options.props,
    },
    global: {
      stubs: {
        "ace-editor": true,
        expandable: {
          template: '<div><slot name="label"></slot><slot></slot></div>',
        },
        ...options.stubs,
      },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

const findByTestId = (wrapper: ReturnType<typeof mount>, testId: string) =>
  wrapper.find(`[data-testid="${testId}"]`);

describe("PluginPropView", () => {
  beforeEach(() => {
    originalMutationObserver = global.MutationObserver;
    global.MutationObserver = jest
      .fn<MutationObserver, [MutationCallback]>()
      .mockImplementation(() => ({
        observe: jest.fn(),
        disconnect: jest.fn(),
        takeRecords: jest.fn(),
      }));
    jest.clearAllMocks();
    mockedClipboard.CopyToClipboard.mockResolvedValue(undefined);
  });

  afterEach(() => {
    global.MutationObserver = originalMutationObserver;
  });

  describe("Boolean type", () => {
    it("shows title when value is true", async () => {
      const wrapper = await createWrapper({
        props: {
          prop: { type: "Boolean", title: "Enable Feature", desc: "Toggle feature" },
          value: "true",
        },
      });

      expect(findByTestId(wrapper, "boolean-prop-title").exists()).toBe(true);
      expect(findByTestId(wrapper, "boolean-prop-title").text()).toContain(
        "Enable Feature",
      );
    });

    it("shows 'yes' text when value is true", async () => {
      const wrapper = await createWrapper({
        props: {
          prop: { type: "Boolean", title: "Feature", desc: "desc" },
          value: "true",
        },
      });

      expect(findByTestId(wrapper, "boolean-true-value").text()).toContain(
        "yes",
      );
    });

    it("applies custom booleanTrueDisplayValueClass to true value", async () => {
      const wrapper = await createWrapper({
        props: {
          prop: {
            type: "Boolean",
            title: "Feature",
            desc: "desc",
            options: { booleanTrueDisplayValueClass: "text-primary" },
          },
          value: "true",
        },
      });

      expect(
        findByTestId(wrapper, "boolean-true-value").classes(),
      ).toContain("text-primary");
    });

    it("uses text-success as default class for true value", async () => {
      const wrapper = await createWrapper({
        props: {
          prop: { type: "Boolean", title: "Feature", desc: "desc" },
          value: "true",
        },
      });

      expect(
        findByTestId(wrapper, "boolean-true-value").classes(),
      ).toContain("text-success");
    });

    it("shows false value when value is false and defaultValue is true", async () => {
      const wrapper = await createWrapper({
        props: {
          prop: {
            type: "Boolean",
            title: "Feature",
            desc: "desc",
            defaultValue: "true",
          },
          value: "false",
        },
      });

      expect(findByTestId(wrapper, "boolean-false-value").exists()).toBe(true);
      expect(findByTestId(wrapper, "boolean-false-value").text()).toContain(
        "no",
      );
    });

    it("hides boolean content when value is false and no defaultValue is true", async () => {
      const wrapper = await createWrapper({
        props: {
          prop: { type: "Boolean", title: "Feature", desc: "desc" },
          value: "false",
        },
      });

      expect(findByTestId(wrapper, "boolean-prop-title").exists()).toBe(false);
    });

    it("applies custom booleanFalseDisplayValueClass to false value", async () => {
      const wrapper = await createWrapper({
        props: {
          prop: {
            type: "Boolean",
            title: "Feature",
            desc: "desc",
            defaultValue: "true",
            options: { booleanFalseDisplayValueClass: "text-danger" },
          },
          value: "false",
        },
      });

      expect(
        findByTestId(wrapper, "boolean-false-value").classes(),
      ).toContain("text-danger");
    });
  });

  describe("Integer type", () => {
    it("shows title for integer property", async () => {
      const wrapper = await createWrapper({
        props: {
          prop: { type: "Integer", title: "Max Count", desc: "Maximum count" },
          value: "42",
        },
      });

      expect(findByTestId(wrapper, "integer-prop-title").text()).toContain(
        "Max Count",
      );
    });

    it("shows integer value", async () => {
      const wrapper = await createWrapper({
        props: {
          prop: { type: "Integer", title: "Max Count", desc: "Maximum count" },
          value: "42",
        },
      });

      expect(findByTestId(wrapper, "integer-prop-value").text()).toBe("42");
    });
  });

  describe("Options type", () => {
    it("shows title for options property", async () => {
      const wrapper = await createWrapper({
        props: {
          prop: { type: "Options", title: "Colors", desc: "Select colors" },
          value: "red, blue, green",
        },
      });

      expect(findByTestId(wrapper, "options-prop-title").text()).toContain(
        "Colors",
      );
    });

    it("renders each comma-separated option as a separate value element", async () => {
      const wrapper = await createWrapper({
        props: {
          prop: { type: "Options", title: "Colors", desc: "Select colors" },
          value: "red, blue, green",
        },
      });

      expect(wrapper.findAll('[data-testid="option-value"]')).toHaveLength(3);
    });

    it("shows the text of each option value", async () => {
      const wrapper = await createWrapper({
        props: {
          prop: { type: "Options", title: "Colors", desc: "Select colors" },
          value: "red, blue",
        },
      });

      const options = wrapper.findAll('[data-testid="option-value"]');
      expect(options[0].text()).toContain("red");
      expect(options[1].text()).toContain("blue");
    });

    it("renders a single option when value has no comma", async () => {
      const wrapper = await createWrapper({
        props: {
          prop: { type: "Options", title: "Colors", desc: "Select colors" },
          value: "red",
        },
      });

      const options = wrapper.findAll('[data-testid="option-value"]');
      expect(options).toHaveLength(1);
      expect(options[0].text()).toContain("red");
    });
  });

  describe("String type (default)", () => {
    it("shows the property title", async () => {
      const wrapper = await createWrapper({
        props: {
          prop: { type: "String", title: "My String", desc: "desc" },
          value: "hello world",
        },
      });

      expect(findByTestId(wrapper, "string-prop-title").text()).toContain(
        "My String",
      );
    });

    it("shows the string value", async () => {
      const wrapper = await createWrapper({
        props: {
          prop: { type: "String", title: "My String", desc: "desc" },
          value: "hello world",
        },
      });

      expect(findByTestId(wrapper, "string-prop-value").text()).toContain(
        "hello world",
      );
    });
  });

  describe("String PASSWORD display type", () => {
    it("shows obfuscated bullet characters instead of real value", async () => {
      const wrapper = await createWrapper({
        props: {
          prop: {
            type: "String",
            title: "Secret",
            desc: "desc",
            options: { displayType: "PASSWORD" },
          },
          value: "mysecretpassword",
        },
      });

      expect(findByTestId(wrapper, "password-prop-value").exists()).toBe(true);
      expect(findByTestId(wrapper, "password-prop-value").text()).not.toContain(
        "mysecretpassword",
      );
      expect(findByTestId(wrapper, "password-prop-value").text()).toContain(
        "••••••••••••",
      );
    });

    it("does not show the string-prop-value element for PASSWORD type", async () => {
      const wrapper = await createWrapper({
        props: {
          prop: {
            type: "String",
            title: "Secret",
            desc: "desc",
            options: { displayType: "PASSWORD" },
          },
          value: "secret",
        },
      });

      expect(findByTestId(wrapper, "string-prop-value").exists()).toBe(false);
    });
  });

  describe("String CODE display type", () => {
    it("shows the property title", async () => {
      const wrapper = await createWrapper({
        props: {
          prop: {
            type: "String",
            title: "Script",
            desc: "Code snippet",
            options: { displayType: "CODE", codeSyntaxMode: "javascript" },
          },
          value: "line1\nline2\nline3",
        },
      });

      expect(findByTestId(wrapper, "code-prop-title").text()).toContain(
        "Script",
      );
    });

    it("shows the computed line count", async () => {
      const wrapper = await createWrapper({
        props: {
          prop: {
            type: "String",
            title: "Script",
            desc: "Code snippet",
            options: { displayType: "CODE", codeSyntaxMode: "javascript" },
          },
          value: "line1\nline2\nline3",
        },
      });

      expect(findByTestId(wrapper, "code-line-count").text()).toContain(
        "3 lines",
      );
    });

    it("shows 1 line for a single-line value", async () => {
      const wrapper = await createWrapper({
        props: {
          prop: {
            type: "String",
            title: "Script",
            desc: "Code snippet",
            options: { displayType: "CODE" },
          },
          value: "single line",
        },
      });

      expect(findByTestId(wrapper, "code-line-count").text()).toContain(
        "1 lines",
      );
    });
  });

  describe("String MULTI_LINE display type", () => {
    it("shows the property title", async () => {
      const wrapper = await createWrapper({
        props: {
          prop: {
            type: "String",
            title: "Description",
            desc: "Long text",
            options: { displayType: "MULTI_LINE" },
          },
          value: "line one\nline two\nline three\nline four",
        },
      });

      expect(findByTestId(wrapper, "multiline-prop-title").text()).toContain(
        "Description",
      );
    });

    it("shows the computed line count", async () => {
      const wrapper = await createWrapper({
        props: {
          prop: {
            type: "String",
            title: "Description",
            desc: "Long text",
            options: { displayType: "MULTI_LINE" },
          },
          value: "line one\nline two\nline three\nline four",
        },
      });

      expect(findByTestId(wrapper, "multiline-line-count").text()).toContain(
        "4 lines",
      );
    });
  });

  describe("DYNAMIC_FORM display type", () => {
    it("renders each custom attribute pair", async () => {
      const wrapper = await createWrapper({
        props: {
          prop: {
            type: "String",
            title: "Config",
            desc: "Dynamic form",
            options: { displayType: "DYNAMIC_FORM" },
          },
          value: JSON.stringify([
            { label: "Host", value: "localhost" },
            { label: "Port", value: "8080" },
          ]),
        },
      });

      expect(wrapper.findAll('[data-testid="configpair"]')).toHaveLength(2);
    });

    it("shows each attribute label and value", async () => {
      const wrapper = await createWrapper({
        props: {
          prop: {
            type: "String",
            title: "Config",
            desc: "Dynamic form",
            options: { displayType: "DYNAMIC_FORM" },
          },
          value: JSON.stringify([
            { label: "Host", value: "localhost" },
            { label: "Port", value: "8080" },
          ]),
        },
      });

      const pairs = wrapper.findAll('[data-testid="configpair"]');
      expect(pairs[0].text()).toContain("Host:");
      expect(pairs[0].text()).toContain("localhost");
      expect(pairs[1].text()).toContain("Port:");
      expect(pairs[1].text()).toContain("8080");
    });

    it("renders no pairs when value is empty", async () => {
      const wrapper = await createWrapper({
        props: {
          prop: {
            type: "String",
            title: "Config",
            desc: "Dynamic form",
            options: { displayType: "DYNAMIC_FORM" },
          },
          value: "",
        },
      });

      expect(wrapper.findAll('[data-testid="configpair"]')).toHaveLength(0);
    });
  });

  describe("prop.desc undefined — regression for RUN-4521", () => {
    it("mounts without throwing when desc is undefined", async () => {
      await expect(
        createWrapper({
          props: {
            prop: { type: "String", title: "Runner Filter", desc: undefined },
            value: "LOCAL",
          },
        }),
      ).resolves.toBeDefined();
    });

    it("renders empty title attribute when desc is undefined", async () => {
      const wrapper = await createWrapper({
        props: {
          prop: { type: "String", title: "Runner Filter", desc: undefined },
          value: "LOCAL",
        },
      });

      expect(findByTestId(wrapper, "string-prop-title").attributes("title")).toBe(
        "",
      );
    });

    it("mounts without throwing when desc is undefined for Boolean type", async () => {
      await expect(
        createWrapper({
          props: {
            prop: { type: "Boolean", title: "Enable", desc: undefined },
            value: "true",
          },
        }),
      ).resolves.toBeDefined();
    });
  });

  describe("copy to clipboard (allowCopy)", () => {
    it("shows copy icon when allowCopy is true", async () => {
      const wrapper = await createWrapper({
        props: {
          prop: { type: "String", title: "Field", desc: "desc" },
          value: "copyable text",
          allowCopy: true,
        },
      });

      const icon = findByTestId(wrapper, "copy-icon").element as HTMLElement;
      expect(icon.style.display).not.toBe("none");
    });

    it("hides copy icon when allowCopy is false", async () => {
      const wrapper = await createWrapper({
        props: {
          prop: { type: "String", title: "Field", desc: "desc" },
          value: "copyable text",
          allowCopy: false,
        },
      });

      const icon = findByTestId(wrapper, "copy-icon").element as HTMLElement;
      expect(icon.style.display).toBe("none");
    });

    it("copies the value to clipboard when clicking string prop value", async () => {
      const wrapper = await createWrapper({
        props: {
          prop: { type: "String", title: "Field", desc: "desc" },
          value: "text to copy",
          allowCopy: true,
        },
      });

      await findByTestId(wrapper, "string-prop-value").trigger("click");
      await flushPromises();

      expect(mockedClipboard.CopyToClipboard).toHaveBeenCalledWith(
        "text to copy",
      );
    });

    it("copies boolean true value to clipboard when clicked", async () => {
      const wrapper = await createWrapper({
        props: {
          prop: { type: "Boolean", title: "Feature", desc: "desc" },
          value: "true",
          allowCopy: true,
        },
      });

      await findByTestId(wrapper, "boolean-true-value").trigger("click");
      await flushPromises();

      expect(mockedClipboard.CopyToClipboard).toHaveBeenCalledWith("true");
    });
  });
});
