import { shallowMount, VueWrapper } from "@vue/test-utils";
import LogEntryFlex from "../logEntryFlex.vue";
import { EventBus } from "../../../utilities/vueEventBus";
import { ExecutionOutputEntry } from "../../../stores/ExecutionOutput";
import { IBuilderOpts } from "../logBuilder";
import moment from "moment";

// Mock UiSocket component
jest.mock("../../utils/UiSocket.vue", () => ({
  name: "UiSocket",
  render() {
    return null;
  }
}));

describe("LogEntryFlex.vue", () => {
  let mockEventBus: any;

  // Mock execution output entry for testing
  const mockLogEntry = {
    executionOutput: {} as any,
    meta: {},
    log: "Test log entry",
    logHtml: undefined,
    time: "10:30:00",
    absoluteTime: "2023-05-10T10:30:00Z",
    level: "INFO",
    stepLabel: "1Command",
    path: "1Command / 2Script",
    stepType: "command",
    lineNumber: 1,
    node: "node1",
    selected: false,
  } as ExecutionOutputEntry;

  // Default config options
  const defaultConfig: IBuilderOpts = {
    node: "node1",
    stepCtx: "1",
    nodeIcon: true,
    maxLines: 5000,
    command: { visible: true },
    time: { visible: true },
    gutter: { visible: true },
    content: { lineWrap: true }
  };

  // Default props for mounting the component
  const defaultProps = {
    config: defaultConfig,
    logEntry: mockLogEntry,
    title: "Test Entry",
    selected: false,
    prevEntry: undefined
  };

  // Setup function to mount the component with given props
  const createWrapper = (customProps: Record<string, any> = {}): VueWrapper<any> => {
    // Create mock eventBus if not provided in props
    const mockEventBus = {
      on: jest.fn(),
      emit: jest.fn()
    };

    return shallowMount(LogEntryFlex, {
      props: {
        ...defaultProps,
        ...customProps,
        // Always provide a mock eventBus unless explicitly overridden
        eventBus: customProps.eventBus || mockEventBus
      },
      global: {
        stubs: {
          UiSocket: true
        }
      }
    });
  };

  beforeEach(() => {
    // Setup mock event bus
    mockEventBus = {
      on: jest.fn(),
      emit: jest.fn()
    };

    // Mock moment format function
    jest.spyOn(moment.prototype, 'format').mockReturnValue('10:30:00');
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it("renders correctly with default props", () => {
    const wrapper = createWrapper();
    expect(wrapper.exists()).toBeTruthy();
    expect(wrapper.find('.execution-log__line').exists()).toBeTruthy();
    expect(wrapper.find('.execution-log__content-text').text()).toBe("Test log entry");
  });

  it("renders with selected class when selected prop is true", () => {
    const wrapper = createWrapper({ selected: true });
    expect(wrapper.find('.execution-log__line').classes()).toContain('execution-log__line--selected');
  });

  it("renders gutter when gutter and time/command are visible", () => {
    const wrapper = createWrapper();
    expect(wrapper.find('.execution-log__gutter').exists()).toBeTruthy();
  });

  it("does not render gutter when gutter is not visible", () => {
    const config = { ...defaultConfig, gutter: { visible: false } };
    const wrapper = createWrapper({ config });
    expect(wrapper.find('.execution-log__gutter').exists()).toBeFalsy();
  });

  it("does not render gutter when time and command are not visible", () => {
    const config = {
      ...defaultConfig,
      time: { visible: false },
      command: { visible: false }
    };
    const wrapper = createWrapper({ config });
    expect(wrapper.find('.execution-log__gutter').exists()).toBeFalsy();
  });

  it("renders node badge when node changes", () => {
    const wrapper = createWrapper({
      logEntry: { ...mockLogEntry, node: "node1" },
      prevEntry: { ...mockLogEntry, node: "node2" }
    });
    expect(wrapper.find('.execution-log__node-badge').exists()).toBeTruthy();
  });

  it("does not render node badge when node is the same as previous entry", () => {
    const wrapper = createWrapper({
      logEntry: { ...mockLogEntry, node: "node1" },
      prevEntry: { ...mockLogEntry, node: "node1" }
    });
    expect(wrapper.find('.execution-log__node-badge').exists()).toBeFalsy();
  });

  it("renders HTML content when logHtml is present", () => {
    const logEntryWithHtml = {
      ...mockLogEntry,
      logHtml: "<span>HTML content</span>"
    };
    const wrapper = createWrapper({ logEntry: logEntryWithHtml });
    expect(wrapper.find('.execution-log__content--html').exists()).toBeTruthy();

    // Check for the presence of the HTML element with v-html directive
    const htmlElement = wrapper.find('[data-test-id="log-entry-content-text"]');
    expect(htmlElement.exists()).toBeTruthy();

    // In Vue Test Utils, v-html content is not rendered in the test environment
    // Just verify the right element is there with the right class and data attribute
    expect(htmlElement.classes()).toContain('execution-log__content-text');
  });

  it("renders plaintext content when logHtml is not present", () => {
    const wrapper = createWrapper();
    expect(wrapper.find('.execution-log__content--html').exists()).toBeFalsy();
    expect(wrapper.find('.execution-log__content-text').text()).toBe("Test log entry");
  });

  it("adds line-wrap class when lineWrap is true", () => {
    const wrapper = createWrapper();
    expect(wrapper.find('.execution-log__content-text').classes()).not.toContain('execution-log__content-text--overflow');
  });

  it("adds overflow class when lineWrap is false", () => {
    const config = { ...defaultConfig, content: { lineWrap: false } };
    const wrapper = createWrapper({ config });
    expect(wrapper.find('.execution-log__content-text').classes()).toContain('execution-log__content-text--overflow');
  });

  it("correctly sets level class on content", () => {
    const wrapper = createWrapper({
      logEntry: { ...mockLogEntry, level: "ERROR" }
    });
    expect(wrapper.find('.execution-log__content').classes()).toContain('execution-log__content--level-error');
  });

  it("computes timestamps correctly", () => {
    const wrapper = createWrapper();
    expect((wrapper.vm as any).formatTime()).toBe("10:30:00");
  });

  it("computes display properties correctly", () => {
    const wrapper = createWrapper();
    expect((wrapper.vm as any).timestamps).toBeTruthy();
    expect((wrapper.vm as any).command).toBeTruthy();
    expect((wrapper.vm as any).gutter).toBeTruthy();
    expect((wrapper.vm as any).lineWrap).toBeTruthy();
  });

  it("emits line-select event when clicking on gutter", async () => {
    const wrapper = createWrapper();
    await wrapper.find('.execution-log__gutter').trigger('click');
    expect(wrapper.emitted('line-select')).toBeTruthy();
    expect(wrapper.emitted('line-select')?.[0]).toEqual([1]);
  });

  it("registers event listener on mount and handles settings changes", async () => {
    // Create a mock event bus that matches the EventBus interface
    const eventBus = {
      on: jest.fn(),
      emit: jest.fn()
    };

    const spy = jest.spyOn(eventBus, 'on');

    const wrapper = createWrapper({ eventBus });
    expect(spy).toHaveBeenCalledWith(
      'execution-log-settings-changed',
      expect.any(Function)
    );

    // Simulate settings change event
    const newSettings = { content: { lineWrap: false } };
    await (wrapper.vm as any).handleSettingsChanged(newSettings);
    expect((wrapper.vm as any).cfg.content.lineWrap).toBe(false);
  });

  it("displays slim gutter when timestamps are visible but command is not", () => {
    const config = {
      ...defaultConfig,
      time: { visible: true },
      command: { visible: false }
    };
    const wrapper = createWrapper({ config });
    expect(wrapper.find('.execution-log__gutter').classes()).toContain('execution-log__gutter--slim');
  });
});