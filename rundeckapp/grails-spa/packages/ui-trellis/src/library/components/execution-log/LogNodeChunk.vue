<template>
  <div
    v-if="entryOutputs && entryOutputs.length > 0"
    class="execution-log__node-chunk"
  >
    <DynamicScroller
      :key="nodeChunkKey"
      ref="scroller"
      :items="entryOutputs"
      :min-item-size="2"
      class="scroller execution-log__chunk"
      key-field="lineNumber"
    >
      <template #default="{ item, index, active }">
        <DynamicScrollerItem
          :item="item"
          :data-index="index"
          :active="active"
          :size-dependencies="[item.log, item.logHtml]"
          :emit-resize="emitResize"
          @resize="scrollToLine()"
        >
          <LogEntryFlex
            :key="index"
            :event-bus="eventBus"
            :title="entryTitle(item)"
            :selected="selectedLine === index + 1"
            :config="opts"
            :prev-entry="index > 0 ? entryOutputs[index - 1] : null"
            :log-entry="item"
            @line-select="onSelectLine"
          />
        </DynamicScrollerItem>
      </template>
    </DynamicScroller>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import type { PropType } from "vue";
import { DynamicScroller, DynamicScrollerItem } from "vue-virtual-scroller";
import { ExecutionOutputEntry } from "../../stores/ExecutionOutput";
import LogEntryFlex from "./logEntryFlex.vue";
import { EventBus } from "../../utilities/vueEventBus";
import { LogBuilder } from "./logBuilder";

export default defineComponent({
  name: "LogNodeChunk",
  components: {
    LogEntryFlex,
    DynamicScroller,
    DynamicScrollerItem,
  },
  props: {
    eventBus: {
      type: Object as PropType<typeof EventBus>,
      required: false,
    },
    selectedLine: {
      type: Number,
      default: undefined,
    },
    node: {
      type: String,
      default: "",
    },
    stepCtx: {
      type: String,
      required: false,
    },
    nodeIcon: {
      type: Boolean,
      required: false,
    },
    maxLine: {
      type: Number,
      default: 2000,
    },
    command: {
      type: Boolean,
      default: false,
    },
    time: {
      type: Boolean,
      default: false,
    },
    gutter: {
      type: Boolean,
      default: false,
    },
    lineWrap: {
      type: Boolean,
      default: false,
    },
    entries: {
      type: Array as PropType<ExecutionOutputEntry[]>,
      required: false,
    },
    jumpToLine: {
      type: Number,
      default: 0,
    },
    jumped: {
      type: Boolean,
      default: false,
    },
    follow: {
      type: Boolean,
      default: false,
    },
  },
  emits: ["line-select", "jumped", "follow-change"],
  data() {
    return {
      emitResize: true as boolean,
    };
  },
  watch: {
    entries(newEntries: ExecutionOutputEntry[] | undefined) {
      if (!this.follow || !newEntries || newEntries.length === 0) return;
      this.$nextTick(() => {
        // Re-check inside $nextTick: the user may have toggled follow off
        // between when this watcher fired and when the DOM updated.
        if (this.follow) {
          (this.$refs.scroller as any)?.scrollToBottom?.();
        }
      });
    },
  },
  computed: {
    opts() {
      return {
        ...LogBuilder.DefaultOpts(),
        node: this.node,
        stepCtx: this.stepCtx,
        nodeIcon: this.nodeIcon,
        maxLines: 20000,
        command: {
          visible: this.command,
        },
        time: {
          visible: this.time,
        },
        gutter: {
          visible: this.gutter,
        },
        content: {
          lineWrap: this.lineWrap,
        },
      };
    },
    entryOutputs() {
      return this.entries
        ? this.entries.map((entry, index) => {
            return this.buildEntry(entry, index);
          })
        : [];
    },
    nodeChunkKey() {
      return `key-${this.nodeIcon}-${this.command}-${this.time}-${this.gutter}-${this.lineWrap}`;
    },
  },
  mounted() {
    if (this.jumpToLine && !this.jumped) {
      this.$emit("line-select", this.jumpToLine);
      this.$emit("jumped");
    }
    this._tryAttachScrollListener();
  },
  updated() {
    // The DynamicScroller is inside a v-if, so it may not exist at mounted().
    // Re-try attaching the listener on every update until it succeeds.
    this._tryAttachScrollListener();
  },
  beforeUnmount() {
    const scrollerEl = (this.$refs.scroller as any)?.$el as HTMLElement | undefined;
    if (scrollerEl) {
      scrollerEl.removeEventListener("scroll", this.onScrollerScroll);
    }
    (this as any)._scrollListenerAdded = false;
  },
  methods: {
    entryTitle(newEntry: ExecutionOutputEntry) {
      return `#${newEntry.lineNumber} ${newEntry.absoluteTime || newEntry.time} ${this.entryPath(newEntry)}`;
    },
    entryPath(newEntry: ExecutionOutputEntry) {
      if (!newEntry.renderedStep) {
        return "";
      }
      const stepString = newEntry.renderedStep.map((s) => {
        if (!s) {
          return "..";
        }
        return `${s.stepNumber}${s.label}`;
      });
      return stepString.join(" / ");
    },
    entryStepLabel(newEntry: ExecutionOutputEntry) {
      if (!newEntry.renderedStep) {
        return "";
      }

      const lastStep = newEntry.renderedStep[newEntry.renderedStep.length - 1];
      return lastStep
        ? `${lastStep.stepNumber}${lastStep.label}`
        : newEntry.stepctx;
    },
    entryStepType(newEntry: ExecutionOutputEntry) {
      if (!newEntry.renderedStep) {
        return "";
      }

      const lastStep = newEntry.renderedStep[newEntry.renderedStep.length - 1];
      return lastStep ? lastStep.type : "";
    },
    buildEntry(entry, index) {
      const newEntry: ExecutionOutputEntry = {
        executionOutput: entry.executionOutput,
        meta: entry.meta,
        log: entry.log,
        logHtml: entry.logHtml,
        time: entry.time,
        absoluteTime: entry.absoluteTime,
        level: entry.level,
        stepLabel: this.entryStepLabel(entry),
        path: this.entryPath(entry),
        stepType: this.entryStepType(entry),
        lineNumber: index + 1,
        node: entry.node,
        selected: this.selectedLine === index + 1,
      };
      return newEntry;
    },
    _tryAttachScrollListener() {
      if ((this as any)._scrollListenerAdded) return;
      const scrollerEl = (this.$refs.scroller as any)?.$el as HTMLElement | undefined;
      if (scrollerEl) {
        scrollerEl.addEventListener("scroll", this.onScrollerScroll);
        (this as any)._scrollListenerAdded = true;
      }
    },
    onSelectLine(index: number) {
      this.$emit("line-select", index);
    },
    onScrollerScroll(ev: Event) {
      const el = ev.target as HTMLElement;
      // If the user scrolled away from the bottom, notify the parent to disable follow.
      const atBottom =
        el.scrollTop + el.clientHeight >= el.scrollHeight - 10;
      if (!atBottom && this.follow) {
        this.$emit("follow-change", false);
      }
    },
    scrollToLine() {
      if (this.follow) {
        this.$refs.scroller.scrollToBottom();
      }
    },
  },
});
</script>

<style scoped>
@import "~vue-virtual-scroller/dist/vue-virtual-scroller.css";
.scroller {
  height: 100%;
}
</style>
