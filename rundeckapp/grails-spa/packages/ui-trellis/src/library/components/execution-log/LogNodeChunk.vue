<template>
  <div class="execution-log__node-chunk" v-if="entryOutputs && entryOutputs.length > 0">
    <DynamicScroller :items="entryOutputs"
                     :min-item-size="2"
                     :key="nodeChunkKey"
                     class="scroller execution-log__chunk"
                     key-field="lineNumber"
                     ref="scroller"

    >
      <template v-slot="{ item, index, active}">
        <DynamicScrollerItem :item="item"
                             :data-index="index"
                             :active="active"
                             :size-dependencies="[item.log, item.logHtml]"
                             :emit-resize="true"
                             @resize="scrollToLine()"
        >
          <LogEntryFlex :eventBus="eventBus"
                        :key="index"
                        :title="entryTitle(item)"
                        :selected="selectedLine === (index + 1)"
                        :config="opts"
                        :prevEntry="(index>0) ? entryOutputs[index - 1] : null"
                        :logEntry="item"
                        @line-select="onSelectLine"
          />
        </DynamicScrollerItem>
      </template>
    </DynamicScroller>
  </div>
</template>

<script lang="ts">
import {defineComponent} from 'vue'
import type {PropType} from 'vue'
import { DynamicScroller, DynamicScrollerItem } from 'vue-virtual-scroller'
import {ExecutionOutputEntry} from "../../stores/ExecutionOutput";
import LogEntryFlex from "./logEntryFlex.vue";
import {EventBus} from "../../utilities/vueEventBus";
import {LogBuilder} from "./logBuilder";

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
      default: ''
    },
    stepCtx: {
      type: String,
      required: false
    },
    nodeIcon: {
      type: Boolean,
      required: false
    },
    maxLine: {
      type: Number,
      default: 2000
    },
    command: {
      type: Boolean,
      default: false
    },
    time: {
      type: Boolean,
      default: false
    },
    gutter: {
      type: Boolean,
      default: false
    },
    lineWrap: {
      type: Boolean,
      default: false
    },
    entries: {
      type: Array as PropType<ExecutionOutputEntry[]>,
      required: false
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
  emits: ['line-select', 'jumped'],
  computed: {
    opts() {
      return {
        ...LogBuilder.DefaultOpts(),
        node: this.node,
        stepCtx: this.stepCtx,
        nodeIcon: this.nodeIcon,
        maxLines: 20000,
        command: {
          visible: this.command
        },
        time: {
          visible: this.time
        },
        gutter: {
          visible: this.gutter
        },
        content: {
          lineWrap: this.lineWrap
        }
      }
    },
    entryOutputs() {
      return this.entries.map((entry, index) => {
        return this.buildEntry(entry, index)
      })
    },
    nodeChunkKey() {
      return `key-${this.nodeIcon}-${this.command}-${this.time}-${this.gutter}-${this.lineWrap}`
    },
  },
  methods: {
    entryTitle(newEntry: ExecutionOutputEntry) {
      return `#${newEntry.lineNumber} ${newEntry.absoluteTime || newEntry.time} ${this.entryPath(newEntry)}`
    },
    entryPath(newEntry: ExecutionOutputEntry) {
      if (!newEntry.renderedStep) { return '' }
      const stepString = newEntry.renderedStep.map( s => {
        if (!s) { return '..' }
        return `${s.stepNumber}${s.label}`
      })
      return stepString.join(' / ')
    },
    entryStepLabel(newEntry: ExecutionOutputEntry) {
      if (!newEntry.renderedStep) { return '' }

      const lastStep = newEntry.renderedStep[newEntry.renderedStep.length -1]
      return lastStep ? `${lastStep.stepNumber}${lastStep.label}` : newEntry.stepctx
    },
    entryStepType(newEntry: ExecutionOutputEntry) {
      if (!newEntry.renderedStep) { return '' }

      const lastStep = newEntry.renderedStep[newEntry.renderedStep.length -1]
      return lastStep ? lastStep.type : ''
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
        selected: this.selectedLine === (index + 1),
      };
      return newEntry
    },
    onSelectLine(index: number) {
      this.$emit('line-select', index)
    },
    scrollToLine() {
      if (this.follow) {
        this.$refs.scroller.scrollToBottom()
      }
    },
  },
})
</script>

<style scoped>
@import "~vue-virtual-scroller/dist/vue-virtual-scroller.css";
.scroller {
  height: 100%;
}
</style>