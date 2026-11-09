import { mount, VueWrapper } from "@vue/test-utils";
import { defineComponent, getCurrentInstance, ref } from "vue";
import LogNodeChunk from "../LogNodeChunk.vue";
import { mockEntries } from "./mocks/logNodeChunkEntries";

jest.mock("vue-virtual-scroller", () => ({
  DynamicScroller: { name: "DynamicScroller", template: "<div />" },
  DynamicScrollerItem: { name: "DynamicScrollerItem", template: "<div />" },
}));

jest.mock("../logEntryFlex.vue", () => ({
  name: "LogEntryFlex",
  template: "<div />",
}));

// Measured content height reported by the virtual scroller (RecycleScroller.totalSize)
const mockTotalSize = ref(0);
// Rows the virtual scroller has not measured yet (DynamicScroller.$_undefinedSizes)
const mockUndefinedSizes = ref(0);
const mockScrollToBottom = jest.fn();
const mockGetItemSize = jest.fn();

// DynamicScroller wraps a RecycleScroller (its "scroller" ref) whose
// totalSize is the summed height of the measured log lines.
const RecycleScrollerStub = defineComponent({
  name: "RecycleScroller",
  computed: {
    totalSize(): number {
      return mockTotalSize.value;
    },
  },
  template: "<div />",
});

const DynamicScrollerStub = defineComponent({
  name: "DynamicScroller",
  components: { RecycleScroller: RecycleScrollerStub },
  props: { items: { type: Array, default: () => [] } },
  setup() {
    // $-prefixed keys are not proxied from data, so expose it on the instance context.
    const { ctx } = getCurrentInstance() as unknown as {
      ctx: Record<string, unknown>;
    };
    Object.defineProperty(ctx, "$_undefinedSizes", {
      get: () => mockUndefinedSizes.value,
      configurable: true,
    });
  },
  methods: {
    scrollToBottom() {
      mockScrollToBottom();
    },
    getItemSize(item: unknown) {
      return mockGetItemSize(item);
    },
  },
  template: `
    <div>
      <RecycleScroller ref="scroller" />
      <template v-for="(item, index) in items" :key="index">
        <slot :item="item" :index="index" :active="true" />
      </template>
    </div>
  `,
});

const DynamicScrollerItemStub = defineComponent({
  name: "DynamicScrollerItem",
  emits: ["resize"],
  template: "<div><slot /></div>",
});

const LogEntryFlexStub = defineComponent({
  name: "LogEntryFlex",
  props: {
    title: { type: String, default: "" },
    config: { type: Object, default: () => ({}) },
    logEntry: { type: Object, default: () => ({}) },
    prevEntry: { type: Object, default: null },
    selected: { type: Boolean, default: false },
    eventBus: { type: Object, default: undefined },
  },
  emits: ["line-select"],
  template:
    '<div data-testid="log-node-chunk-entry" :title="title">{{ logEntry.log }}</div>',
});

interface MountOptions {
  props?: Record<string, unknown>;
}

const createWrapper = (options: MountOptions = {}): VueWrapper<any> => {
  return mount(LogNodeChunk, {
    props: {
      node: "node1",
      entries: mockEntries,
      nodeIcon: true,
      command: true,
      time: true,
      gutter: true,
      lineWrap: true,
      jumpToLine: 0,
      jumped: false,
      follow: false,
      ...options.props,
    },
    global: {
      stubs: {
        DynamicScroller: DynamicScrollerStub,
        DynamicScrollerItem: DynamicScrollerItemStub,
        LogEntryFlex: LogEntryFlexStub,
      },
    },
  });
};

const findByTestId = (wrapper: VueWrapper<any>, testId: string) =>
  wrapper.find(`[data-testid="${testId}"]`);

const findEntries = (wrapper: VueWrapper<any>) =>
  wrapper.findAllComponents(LogEntryFlexStub);

/** Simulates the user scrolling the log viewport to the given position. */
const scrollTo = async (
  wrapper: VueWrapper<any>,
  scrollTop: number,
  clientHeight = 500,
  scrollHeight = 1000,
) => {
  const scroller = findByTestId(wrapper, "log-node-chunk-scroller");
  const el = scroller.element as HTMLElement;
  Object.defineProperty(el, "scrollTop", {
    value: scrollTop,
    configurable: true,
  });
  Object.defineProperty(el, "clientHeight", {
    value: clientHeight,
    configurable: true,
  });
  Object.defineProperty(el, "scrollHeight", {
    value: scrollHeight,
    configurable: true,
  });
  await scroller.trigger("scroll");
};

// requestAnimationFrame queue, flushed by the tests to step the follow pin
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
  jest.spyOn(window, "cancelAnimationFrame").mockImplementation((id) => {
    pendingFrames.delete(id);
  });
};

const flushFrames = (max = 12) => {
  let steps = 0;
  while (pendingFrames.size > 0 && steps < max) {
    const [id, cb] = pendingFrames.entries().next().value as [
      number,
      FrameRequestCallback,
    ];
    pendingFrames.delete(id);
    cb(0);
    steps += 1;
  }
};

/** Gives the log viewport a scroll position and a (possibly changing) height. */
const prepareViewport = (
  wrapper: VueWrapper<any>,
  scrollTop = 120,
  scrollHeight: () => number = () => 4000,
) => {
  const el = findByTestId(wrapper, "log-node-chunk-scroller")
    .element as HTMLElement;
  Object.defineProperty(el, "scrollHeight", {
    configurable: true,
    get: scrollHeight,
  });
  el.scrollTop = scrollTop;
  return el;
};

const settlePin = async (wrapper: VueWrapper<any>) => {
  await wrapper.vm.$nextTick();
  flushFrames();
};

const extraEntry = { ...mockEntries[0], log: "another line", lineNumber: 4 };

describe("LogNodeChunk", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockTotalSize.value = 0;
    mockUndefinedSizes.value = 0;
    installFrameMock();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  describe("Rendering", () => {
    it("renders one entry per log line", () => {
      const wrapper = createWrapper();

      const entries = findEntries(wrapper);
      expect(entries).toHaveLength(3);
      expect(entries[0].text()).toBe("Test log entry 1");
      expect(entries[2].text()).toBe("Test log entry 3");
    });

    it("does not render when entries are empty", () => {
      const wrapper = createWrapper({ props: { entries: [] } });

      expect(findByTestId(wrapper, "log-node-chunk").exists()).toBe(false);
    });

    it("does not render when entries are null", () => {
      const wrapper = createWrapper({ props: { entries: null } });

      expect(findByTestId(wrapper, "log-node-chunk").exists()).toBe(false);
    });

    it("passes the display settings to each entry", () => {
      const wrapper = createWrapper({
        props: {
          node: "testNode",
          stepCtx: "testStep",
          command: false,
          time: true,
          gutter: false,
          lineWrap: true,
        },
      });

      expect(findEntries(wrapper)[0].props("config")).toEqual(
        expect.objectContaining({
          node: "testNode",
          stepCtx: "testStep",
          nodeIcon: true,
          command: { visible: false },
          time: { visible: true },
          gutter: { visible: false },
          content: { lineWrap: true },
        }),
      );
    });

    it("titles each entry with its line number and time", () => {
      const wrapper = createWrapper();

      expect(findEntries(wrapper)[0].attributes("title")).toBe(
        "#1 2023-05-10T10:30:00Z ",
      );
      expect(findEntries(wrapper)[1].attributes("title")).toBe(
        "#2 2023-05-10T10:30:01Z ",
      );
    });

    it("passes each entry its step label, step type and line number", () => {
      const wrapper = createWrapper();

      expect(findEntries(wrapper)[0].props("logEntry")).toEqual(
        expect.objectContaining({
          log: "Test log entry 1",
          time: "10:30:00",
          absoluteTime: "2023-05-10T10:30:00Z",
          level: "INFO",
          stepLabel: "2Step Two",
          stepType: "script",
          lineNumber: 1,
          node: "node1",
          selected: false,
        }),
      );
    });

    it("marks the selected line", () => {
      const wrapper = createWrapper({ props: { selectedLine: 2 } });

      const entries = findEntries(wrapper);
      expect(entries[0].props("selected")).toBe(false);
      expect(entries[1].props("selected")).toBe(true);
    });
  });

  describe("Line selection", () => {
    it("emits line-select when the user selects an entry", async () => {
      const wrapper = createWrapper();

      await findEntries(wrapper)[2].vm.$emit("line-select", 3);

      expect(wrapper.emitted("line-select")).toEqual([[3]]);
    });

    it("emits line-select and jumped on mount when jumpToLine is provided", async () => {
      const wrapper = createWrapper({
        props: { jumpToLine: 2, jumped: false },
      });
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("line-select")).toEqual([[2]]);
      expect(wrapper.emitted("jumped")).toHaveLength(1);
    });

    it("does not jump again once it has already jumped", async () => {
      const wrapper = createWrapper({ props: { jumpToLine: 2, jumped: true } });
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("line-select")).toBeUndefined();
      expect(wrapper.emitted("jumped")).toBeUndefined();
    });
  });

  describe("Follow", () => {
    it.each([
      [true, 9000],
      [false, 120],
    ])(
      "pins the viewport to the bottom when an entry resizes and follow is %p",
      async (follow: boolean, scrollTop: number) => {
        const wrapper = createWrapper({ props: { follow } });
        const el = prepareViewport(wrapper);

        await wrapper
          .findAllComponents(DynamicScrollerItemStub)[0]
          .vm.$emit("resize");
        await settlePin(wrapper);

        expect(el.scrollTop).toBe(scrollTop);
        expect(mockScrollToBottom).not.toHaveBeenCalled();
      },
    );

    it("does not pin to the bottom when entries arrive with follow off", async () => {
      const wrapper = createWrapper({ props: { follow: false } });
      const el = prepareViewport(wrapper);

      await wrapper.setProps({ entries: [...mockEntries, extraEntry] });
      await settlePin(wrapper);

      expect(el.scrollTop).toBe(120);
    });

    it("pins to the bottom when entries arrive with follow on", async () => {
      const wrapper = createWrapper({ props: { follow: true } });
      const el = prepareViewport(wrapper);

      await wrapper.setProps({ entries: [...mockEntries, extraEntry] });
      await settlePin(wrapper);

      expect(el.scrollTop).toBe(9000);
    });

    it("keeps pinning until the visible rows are measured", async () => {
      let height = 100;
      mockUndefinedSizes.value = 4;
      const wrapper = createWrapper({ props: { follow: true } });
      const el = prepareViewport(wrapper, 0, () => height);

      await wrapper.setProps({ entries: [...mockEntries, extraEntry] });
      await wrapper.vm.$nextTick();
      flushFrames(4);

      expect(el.scrollTop).toBe(5100);

      height = 4000;
      mockUndefinedSizes.value = 0;
      flushFrames();

      expect(el.scrollTop).toBe(9000);
      expect(pendingFrames.size).toBe(0);
    });

    it("stops pinning after follow is turned off", async () => {
      const wrapper = createWrapper({ props: { follow: true } });
      const el = prepareViewport(wrapper);

      await wrapper.setProps({ follow: false });
      await settlePin(wrapper);

      expect(el.scrollTop).toBe(120);
    });

    it("stops a pin that is still measuring when follow is turned off", async () => {
      let height = 100;
      mockUndefinedSizes.value = 4;
      const wrapper = createWrapper({ props: { follow: true } });
      const el = prepareViewport(wrapper, 0, () => height);

      await wrapper.setProps({ entries: [...mockEntries, extraEntry] });
      await wrapper.vm.$nextTick();
      expect(el.scrollTop).toBe(5100);

      await wrapper.setProps({ follow: false });
      height = 8000;
      flushFrames();

      expect(el.scrollTop).toBe(5100);
      expect(pendingFrames.size).toBe(0);
    });

    it("pins to the bottom when follow is turned on", async () => {
      const wrapper = createWrapper({ props: { follow: false } });
      const el = prepareViewport(wrapper);

      await wrapper.setProps({ follow: true });
      await settlePin(wrapper);

      expect(el.scrollTop).toBe(9000);
    });

    // RUN-4814: on a large, actively-streaming log, DynamicScroller's
    // scrollHeight underestimates the real content height while trailing
    // entries are still unmeasured, so the naive "am I at the bottom?"
    // check can fire long before the user actually reaches the last line.
    it("disables follow when the user scrolls up", async () => {
      const wrapper = createWrapper({ props: { follow: true } });

      await scrollTo(wrapper, 500);
      await scrollTo(wrapper, 400);

      expect(wrapper.emitted("follow-change")).toEqual([[false]]);
    });

    it("does not re-enable follow at the bottom while the last entry is still unmeasured", async () => {
      mockGetItemSize.mockReturnValue(0);
      const wrapper = createWrapper({ props: { follow: false } });

      await scrollTo(wrapper, 490);
      await scrollTo(wrapper, 500);

      expect(wrapper.emitted("follow-change")).toBeUndefined();
    });

    it("re-enables follow at the bottom once the last entry has been measured", async () => {
      mockGetItemSize.mockReturnValue(20);
      const wrapper = createWrapper({ props: { follow: false } });

      await scrollTo(wrapper, 490);
      await scrollTo(wrapper, 500);

      expect(wrapper.emitted("follow-change")).toEqual([[true]]);
    });
  });

  describe("Content size", () => {
    it("emits the measured content height when it changes", async () => {
      const wrapper = createWrapper();

      mockTotalSize.value = 240;
      await wrapper.vm.$nextTick();
      mockTotalSize.value = 240;
      await wrapper.vm.$nextTick();
      mockTotalSize.value = 480;
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("content-size")).toEqual([[240], [480]]);
    });

    it("does not emit before any content has been measured", async () => {
      const wrapper = createWrapper();
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("content-size")).toBeUndefined();
    });

    it("keeps reporting the content height after display settings change", async () => {
      const wrapper = createWrapper();
      mockTotalSize.value = 240;
      await wrapper.vm.$nextTick();

      await wrapper.setProps({ gutter: false });
      await wrapper.vm.$nextTick();
      mockTotalSize.value = 300;
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("content-size")?.at(-1)).toEqual([300]);
    });
  });
});
