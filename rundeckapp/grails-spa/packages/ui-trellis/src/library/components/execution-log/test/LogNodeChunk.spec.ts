import { shallowMount, VueWrapper } from "@vue/test-utils";
import LogNodeChunk from "../LogNodeChunk.vue";
import { EventBus } from "../../../utilities/vueEventBus";
import { ExecutionOutputEntry } from "../../../stores/ExecutionOutput";

// Mock components used by LogNodeChunk
jest.mock("vue-virtual-scroller", () => ({
  DynamicScroller: {
    name: "DynamicScroller",
    render() {
      return this.$slots.default
        ? this.$slots.default({
            item: {},
            index: 0,
            active: true,
          })
        : null;
    },
  },
  DynamicScrollerItem: {
    name: "DynamicScrollerItem",
    render() {
      return this.$slots.default ? this.$slots.default() : null;
    },
  },
}));

// Mock LogEntryFlex component
jest.mock("../logEntryFlex.vue", () => ({
  name: "LogEntryFlex",
  render() {
    return null;
  },
}));

describe("LogNodeChunk.vue", () => {
  let mockEventBus: typeof EventBus;

  // Sample execution output entries for testing
  const mockEntries: ExecutionOutputEntry[] = [
    {
      executionOutput: {} as any,
      meta: {},
      log: "Test log entry 1",
      time: "10:30:00",
      absoluteTime: "2023-05-10T10:30:00Z",
      level: "INFO",
      lineNumber: 1,
      node: "node1",
    },
    {
      executionOutput: {} as any,
      meta: {},
      log: "Test log entry 2",
      time: "10:30:01",
      absoluteTime: "2023-05-10T10:30:01Z",
      level: "INFO",
      lineNumber: 2,
      node: "node1",
    },
    {
      executionOutput: {} as any,
      meta: {},
      log: "Test log entry 3",
      time: "10:30:02",
      absoluteTime: "2023-05-10T10:30:02Z",
      level: "INFO",
      lineNumber: 3,
      node: "node1",
    },
  ];

  // Default props for mounting the component
  const defaultProps = {
    node: "node1",
    entries: mockEntries,
    selectedLine: undefined,
    nodeIcon: true,
    command: true,
    time: true,
    gutter: true,
    lineWrap: true,
    jumpToLine: 0,
    jumped: false,
    follow: false,
  };

  // Setup function to mount the component with given props
  const createWrapper = (props = {}): VueWrapper<any> => {
    return shallowMount(LogNodeChunk, {
      props: {
        ...defaultProps,
        ...props,
      },
      global: {
        stubs: {
          DynamicScroller: true,
          DynamicScrollerItem: true,
          LogEntryFlex: true,
        },
      },
    });
  };

  beforeEach(() => {
    mockEventBus = {
      on: jest.fn(),
      emit: jest.fn(),
    } as unknown as typeof EventBus;
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it("renders when entries are provided", () => {
    const wrapper = createWrapper();
    expect(wrapper.exists()).toBeTruthy();
    expect(wrapper.find(".execution-log__node-chunk").exists()).toBeTruthy();
  });

  it("does not render when entries are empty", () => {
    const wrapper = createWrapper({ entries: [] });
    expect(wrapper.find(".execution-log__node-chunk").exists()).toBeFalsy();
  });
  it("does not render when entries are null", () => {
    const wrapper = createWrapper({ entries: null });
    expect(wrapper.find(".execution-log__node-chunk").exists()).toBeFalsy();
  });

  it("computes nodeChunkKey correctly", () => {
    const wrapper = createWrapper({
      nodeIcon: true,
      command: true,
      time: true,
      gutter: true,
      lineWrap: true,
    });
    expect((wrapper.vm as any).nodeChunkKey).toBe("key-true-true-true-true-true");

    const wrapper2 = createWrapper({
      nodeIcon: false,
      command: false,
      time: false,
      gutter: false,
      lineWrap: false,
    });
    expect((wrapper2.vm as any).nodeChunkKey).toBe("key-false-false-false-false-false");
  });

  it("computes entryOutputs correctly", () => {
    const wrapper = createWrapper();
    const entryOutputs = (wrapper.vm as any).entryOutputs;

    expect(entryOutputs).toHaveLength(3);
    expect(entryOutputs[0].lineNumber).toBe(1);
    expect(entryOutputs[1].lineNumber).toBe(2);
    expect(entryOutputs[2].lineNumber).toBe(3);
  });

  it("computes opts correctly", () => {
    const wrapper = createWrapper({
      node: "testNode",
      stepCtx: "testStep",
      nodeIcon: true,
      command: true,
      time: true,
      gutter: true,
      lineWrap: true,
    });

    const opts = (wrapper.vm as any).opts;
    expect(opts).toEqual(expect.objectContaining({
      node: "testNode",
      stepCtx: "testStep",
      nodeIcon: true,
      command: { visible: true },
      time: { visible: true },
      gutter: { visible: true },
      content: { lineWrap: true },
    }));
  });

  it("emits line-select and jumped events when jumpToLine is provided", async () => {
    const wrapper = createWrapper({
      jumpToLine: 2,
      jumped: false,
    });

    await wrapper.vm.$nextTick();
    expect(wrapper.emitted("line-select")).toBeTruthy();
    expect(wrapper.emitted("line-select")?.[0]).toEqual([2]);
    expect(wrapper.emitted("jumped")).toBeTruthy();
  });

  it("formats entry title correctly", () => {
    const wrapper = createWrapper();
    const entry = {
      lineNumber: 1,
      absoluteTime: "2023-05-10T10:30:00Z",
      time: "10:30:00",
      renderedStep: [
        { stepNumber: 1, label: "Step One", type: "command" },
        { stepNumber: 2, label: "Step Two", type: "script" },
      ],
    };

    const title = (wrapper.vm as any).entryTitle(entry);
    expect(title).toBe("#1 2023-05-10T10:30:00Z 1Step One / 2Step Two");
  });

  it("builds entry object correctly", () => {
    const wrapper = createWrapper();
    const entry = {
      executionOutput: {} as any,
      meta: {},
      log: "Test log",
      logHtml: undefined,
      time: "10:30:00",
      absoluteTime: "2023-05-10T10:30:00Z",
      level: "INFO",
      renderedStep: [
        { stepNumber: 1, label: "Step One", type: "command" },
      ],
      node: "node1",
    };

    const builtEntry = (wrapper.vm as any).buildEntry(entry, 0);
    expect(builtEntry).toEqual(expect.objectContaining({
      log: "Test log",
      time: "10:30:00",
      absoluteTime: "2023-05-10T10:30:00Z",
      level: "INFO",
      stepLabel: "1Step One",
      stepType: "command",
      lineNumber: 1,
      node: "node1",
      selected: false,
    }));
  });

  it("handles onSelectLine correctly", () => {
    const wrapper = createWrapper();
    (wrapper.vm as any).onSelectLine(3);
    expect(wrapper.emitted("line-select")?.[0]).toEqual([3]);
  });

  it.each([true,false])("scrolls to bottom when follow is %p", (follow:boolean) => {
    const scrollToBottomMock = jest.fn();
    const wrapper = createWrapper({ follow });

    // Mock the $refs.scroller
    (wrapper.vm as any).$refs.scroller.scrollToBottom = scrollToBottomMock;

    (wrapper.vm as any).scrollToLine();
    expect(scrollToBottomMock).toHaveBeenCalledTimes(follow?1:0);
  });


});