import { mount, VueWrapper } from "@vue/test-utils";
import { defineComponent, ref } from "vue";
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

describe("LogNodeChunk", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockTotalSize.value = 0;
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
      [true, 1],
      [false, 0],
    ])(
      "scrolls to the bottom when an entry resizes and follow is %p",
      async (follow: boolean, calls: number) => {
        const wrapper = createWrapper({ props: { follow } });

        await wrapper
          .findAllComponents(DynamicScrollerItemStub)[0]
          .vm.$emit("resize");

        expect(mockScrollToBottom).toHaveBeenCalledTimes(calls);
      },
    );

    it("scrolls to the bottom when new entries arrive while following", async () => {
      const wrapper = createWrapper({ props: { follow: true } });

      await wrapper.setProps({ entries: mockEntries.slice(0, 2) });
      await wrapper.vm.$nextTick();

      expect(mockScrollToBottom).toHaveBeenCalledTimes(1);
    });

    it("scrolls to the bottom when follow is re-enabled", async () => {
      const wrapper = createWrapper({ props: { follow: false } });

      await wrapper.setProps({ follow: true });
      await wrapper.vm.$nextTick();

      expect(mockScrollToBottom).toHaveBeenCalledTimes(1);
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
