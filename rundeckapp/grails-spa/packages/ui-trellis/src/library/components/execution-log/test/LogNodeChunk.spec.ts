import { shallowMount, VueWrapper } from "@vue/test-utils";
import LogNodeChunk from "../LogNodeChunk.vue";
import { EventBus } from "../../../utilities/vueEventBus";
import { ExecutionOutputEntry } from "../../../stores/ExecutionOutput";

// Mock components used by LogNodeChunk
jest.mock("vue-virtual-scroller", () => ({
  DynamicScroller: {
    name: "DynamicScroller",
    render(this: { $slots: Record<string, Function | undefined> }) {
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
    render(this: { $slots: Record<string, Function | undefined> }) {
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

  const pendingFrames = new Map<number, FrameRequestCallback>();
  let nextFrameId = 0;

  const installFrameMock = () => {
    pendingFrames.clear();
    nextFrameId = 0;
    jest.spyOn(window, "requestAnimationFrame").mockImplementation((cb) => {
      nextFrameId += 1;
      pendingFrames.set(nextFrameId, cb);
      return nextFrameId;
    });
    jest
      .spyOn(window, "cancelAnimationFrame")
      .mockImplementation((id: number) => {
        pendingFrames.delete(id);
      });
  };

  const flushFrames = (max = 12) => {
    let steps = 0;
    while (pendingFrames.size > 0 && steps < max) {
      const next = pendingFrames.entries().next().value as
        | [number, FrameRequestCallback]
        | undefined;
      if (!next) {
        break;
      }
      pendingFrames.delete(next[0]);
      next[1](0);
      steps += 1;
    }
    return steps;
  };

  const settlePin = async (wrapper: VueWrapper<any>) => {
    await wrapper.vm.$nextTick();
    flushFrames();
  };

  beforeEach(() => {
    mockEventBus = {
      on: jest.fn(),
      emit: jest.fn(),
    } as unknown as typeof EventBus;
    installFrameMock();
  });

  afterEach(() => {
    jest.restoreAllMocks();
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
    expect((wrapper.vm as any).nodeChunkKey).toBe(
      "key-true-true-true-true-true",
    );

    const wrapper2 = createWrapper({
      nodeIcon: false,
      command: false,
      time: false,
      gutter: false,
      lineWrap: false,
    });
    expect((wrapper2.vm as any).nodeChunkKey).toBe(
      "key-false-false-false-false-false",
    );
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
    expect(opts).toEqual(
      expect.objectContaining({
        node: "testNode",
        stepCtx: "testStep",
        nodeIcon: true,
        command: { visible: true },
        time: { visible: true },
        gutter: { visible: true },
        content: { lineWrap: true },
      }),
    );
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
      renderedStep: [{ stepNumber: 1, label: "Step One", type: "command" }],
      node: "node1",
    };

    const builtEntry = (wrapper.vm as any).buildEntry(entry, 0);
    expect(builtEntry).toEqual(
      expect.objectContaining({
        log: "Test log",
        time: "10:30:00",
        absoluteTime: "2023-05-10T10:30:00Z",
        level: "INFO",
        stepLabel: "1Step One",
        stepType: "command",
        lineNumber: 1,
        node: "node1",
        selected: false,
      }),
    );
  });

  it("handles onSelectLine correctly", () => {
    const wrapper = createWrapper();
    (wrapper.vm as any).onSelectLine(3);
    expect(wrapper.emitted("line-select")?.[0]).toEqual([3]);
  });

  /**
   * Point the scroller ref at a plain element so tests can read scrollTop.
   */
  const stubScroller = (
    wrapper: VueWrapper<any>,
    scrollTop = 120,
    scrollHeight = 4000,
  ) => {
    const scroller = (wrapper.vm as any).$refs.scroller;
    const scrollToBottom = jest.fn();
    scroller.scrollToBottom = scrollToBottom;
    scroller.$_undefinedSizes = 0;
    scroller.getItemSize = jest.fn().mockReturnValue(20);
    const el = scroller.$el as HTMLElement;
    Object.defineProperty(el, "scrollHeight", {
      configurable: true,
      get: () => scrollHeight,
    });
    el.scrollTop = scrollTop;
    return { el, scrollToBottom, scroller };
  };

  it.each([true, false])(
    "pins the viewport to the bottom when follow is %p",
    async (follow: boolean) => {
      const wrapper = createWrapper({ follow });
      const { el, scrollToBottom } = stubScroller(wrapper);

      (wrapper.vm as any).scrollToLine();
      await settlePin(wrapper);

      expect(scrollToBottom).not.toHaveBeenCalled();
      expect(el.scrollTop).toBe(follow ? 9000 : 120);
    },
  );

  const extraEntry = {
    ...mockEntries[0],
    log: "another line",
    lineNumber: 4,
  };

  it("does not pin to the bottom when entries arrive with follow off", async () => {
    const wrapper = createWrapper({ follow: false });
    const { el, scrollToBottom } = stubScroller(wrapper);

    await wrapper.setProps({
      entries: [...mockEntries, extraEntry],
    });
    await settlePin(wrapper);

    expect(scrollToBottom).not.toHaveBeenCalled();
    expect(el.scrollTop).toBe(120);
  });

  it("pins to the bottom when entries arrive with follow on", async () => {
    const wrapper = createWrapper({ follow: true });
    const { el, scrollToBottom } = stubScroller(wrapper);

    await wrapper.setProps({
      entries: [...mockEntries, extraEntry],
    });
    await settlePin(wrapper);

    expect(scrollToBottom).not.toHaveBeenCalled();
    expect(el.scrollTop).toBe(9000);
  });

  it("keeps pinning until visible rows are measured", async () => {
    const wrapper = createWrapper({ follow: true });
    const { el, scroller } = stubScroller(wrapper, 0, 100);
    scroller.$_undefinedSizes = 4;
    let height = 100;
    Object.defineProperty(el, "scrollHeight", {
      configurable: true,
      get: () => height,
    });

    await wrapper.setProps({
      entries: [...mockEntries, extraEntry],
    });
    await wrapper.vm.$nextTick();
    flushFrames(4);

    expect(el.scrollTop).toBe(5100);

    height = 4000;
    scroller.$_undefinedSizes = 0;
    flushFrames();

    expect(el.scrollTop).toBe(9000);
    expect(pendingFrames.size).toBe(0);
  });

  it("stops pinning after follow is turned off", async () => {
    const wrapper = createWrapper({ follow: true });
    const { el, scrollToBottom } = stubScroller(wrapper);

    await wrapper.setProps({ follow: false });
    await settlePin(wrapper);

    expect(scrollToBottom).not.toHaveBeenCalled();
    expect(el.scrollTop).toBe(120);
  });

  it("stops a pin that is still measuring when follow is turned off", async () => {
    const wrapper = createWrapper({ follow: true });
    const { el, scroller } = stubScroller(wrapper, 0, 100);
    scroller.$_undefinedSizes = 4;

    await wrapper.setProps({
      entries: [...mockEntries, extraEntry],
    });
    await wrapper.vm.$nextTick();
    expect(el.scrollTop).toBe(5100);

    await wrapper.setProps({ follow: false });
    Object.defineProperty(el, "scrollHeight", {
      configurable: true,
      get: () => 8000,
    });
    flushFrames();

    expect(el.scrollTop).toBe(5100);
    expect(pendingFrames.size).toBe(0);
  });

  it("pins to the bottom when follow is turned on", async () => {
    const wrapper = createWrapper({ follow: false });
    const { el, scrollToBottom } = stubScroller(wrapper);

    await wrapper.setProps({ follow: true });
    await settlePin(wrapper);

    expect(scrollToBottom).not.toHaveBeenCalled();
    expect(el.scrollTop).toBe(9000);
  });

  describe("_isLastEntryMeasured", () => {
    it("returns true when there are no entries", () => {
      const wrapper = createWrapper({ entries: [] });
      expect((wrapper.vm as any)._isLastEntryMeasured()).toBe(true);
    });

    it("returns false when the last entry has not been measured yet", () => {
      const wrapper = createWrapper();
      (wrapper.vm as any).$refs.scroller.getItemSize = jest
        .fn()
        .mockReturnValue(0);
      expect((wrapper.vm as any)._isLastEntryMeasured()).toBe(false);
    });

    it("returns true once the last entry has been measured", () => {
      const wrapper = createWrapper();
      (wrapper.vm as any).$refs.scroller.getItemSize = jest
        .fn()
        .mockReturnValue(20);
      expect((wrapper.vm as any)._isLastEntryMeasured()).toBe(true);
    });
  });

  describe("onScrollerScroll", () => {
    // RUN-4814: on a large, actively-streaming log, DynamicScroller's
    // scrollHeight underestimates the real content height while trailing
    // entries are still unmeasured, so the naive "am I at the bottom?"
    // check can fire long before the user actually reaches the last line.
    const scrollEvent = (
      scrollTop: number,
      clientHeight = 500,
      scrollHeight = 1000,
    ) =>
      ({
        target: { scrollTop, clientHeight, scrollHeight },
      }) as unknown as Event;

    it("disables follow when the user scrolls up", () => {
      const wrapper = createWrapper({ follow: true });
      const vm = wrapper.vm as any;

      vm.onScrollerScroll(scrollEvent(500)); // primes _prevScrollTop
      vm.onScrollerScroll(scrollEvent(400)); // scrolls up

      expect(wrapper.emitted("follow-change")?.[0]).toEqual([false]);
    });

    it("does not re-enable follow at the bottom while the last entry is still unmeasured", () => {
      const wrapper = createWrapper({ follow: false });
      const vm = wrapper.vm as any;
      vm.$refs.scroller.getItemSize = jest.fn().mockReturnValue(0);

      vm.onScrollerScroll(scrollEvent(490)); // primes _prevScrollTop
      vm.onScrollerScroll(scrollEvent(500)); // 500 + 500 >= 1000 - 10 -> at bottom

      expect(wrapper.emitted("follow-change")).toBeFalsy();
    });

    it("re-enables follow at the bottom once the last entry has been measured", () => {
      const wrapper = createWrapper({ follow: false });
      const vm = wrapper.vm as any;
      vm.$refs.scroller.getItemSize = jest.fn().mockReturnValue(20);

      vm.onScrollerScroll(scrollEvent(490)); // primes _prevScrollTop
      vm.onScrollerScroll(scrollEvent(500)); // 500 + 500 >= 1000 - 10 -> at bottom

      expect(wrapper.emitted("follow-change")?.[0]).toEqual([true]);
    });
  });
});
