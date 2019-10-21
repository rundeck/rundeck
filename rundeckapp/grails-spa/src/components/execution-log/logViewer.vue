<template>
  <div class="execution-log"
    v-if="this.$el.parentNode.display != 'none'"
    v-bind:class="{'execution-log--dark': this.theme == 'dark', 'execution-log--light': this.theme == 'light' }"
  >
    <div class="execution-log__controls">
      Consume:<input type="checkbox" v-model="consumeLogs"
      />&nbsp;&nbsp;&nbsp;&nbsp;Stats:<input type="checkbox" v-model="showStats"
      >&nbsp;&nbsp;&nbsp;&nbsp;Follow:<input type="checkbox" v-model="follow"
      />&nbsp;&nbsp;&nbsp;&nbsp;Theme:<select v-model="theme">
        <option disabled value="">Theme</option>
        <option>dark</option>
        <option>light</option>
        <option>none</option>
      </select
      >&nbsp;&nbsp;&nbsp;&nbsp;Jump:<input v-model="jumpToLine" v-on:keydown.enter="handleJump"
      ><button v-on:click="handleJumpToStart">Start</button><button v-on:click="handleJumpToEnd">End</button>
    </div>
    <div class="stats" v-if="showStats">
      <span>Following:{{follow}} Lines:{{vues.length.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",")}} TotalTime:{{totalTime/1000}}s</span>
    </div>
    <div ref="scroller" class="execution-log__scroller"/>
  </div>
</template>

<script lang="ts">
import {ExecutionLog} from '@/utilities/ExecutionLogConsumer'
import { Component, Prop, Watch, Vue } from 'vue-property-decorator'
import { RecycleScroller, DynamicScroller, DynamicScrollerItem } from 'vue-virtual-scroller'
import Entry from './logEntry.vue'
import EntryFlex from './logEntryFlex.vue'
import { ExecutionOutputGetResponse } from 'ts-rundeck/dist/lib/models'

import {LogBuilder} from './logBuilder'

Vue.component("recycle-scroller",RecycleScroller)
Vue.component("dynamic-scroller",DynamicScroller)
Vue.component("dynamic-sroller-item",DynamicScrollerItem)

@Component
export default class LogViewer extends Vue {
    @Prop()
    executionId!: number

    @Prop()
    consumeLogs: boolean = true

    @Prop()
    showStats = true

    @Prop()
    follow = false

    @Prop()
    jumpToLine?: number

    @Prop({default: 'dark'})
    theme?: string

    scrollTolerance = 5

    batchSize = 200

    totalTime = 0

    private jumped = false

    private viewer!: ExecutionLog

    private logBuilder!: LogBuilder

    private logEntries: Array<{log?: string, id: number}> = []

    private scrollCount = 0

    private startTime = 0

    private resp?: Promise<ExecutionOutputGetResponse>

    private populateLogsProm?: Promise<void>

    private vues: any[] = []

    @Watch('consumeLogs')
    toggleConsumeLogs(val: boolean, oldVal: boolean) {
      if(val && !this.populateLogsProm) {
        this.populateLogsProm = this.populateLogs()
      }
    }

    async mounted() {
        const scroller = this.$refs["scroller"] as HTMLElement
        this.viewer = new ExecutionLog(this.executionId.toString())
        this.logBuilder = new LogBuilder(scroller, {nodeIcon: true})
        this.startTime = Date.now()
        this.addScrollBlocker()
        this.populateLogsProm = this.populateLogs()
    }

    private addScrollBlocker() {
        const scroller = this.$refs["scroller"] as HTMLElement
        scroller.addEventListener('wheel', (ev: UIEvent) => {
            this.scrollCount++

            if (this.follow) {
                ev.preventDefault()
                ev.returnValue = false
            }
            
            if (this.scrollCount > this.scrollTolerance)
                this.follow = false
        }, {passive: false})
    }

    private handleExecutionLogResp(res: ExecutionOutputGetResponse) {
      // let count = this.logEntries.length

      // const newEntries = res.entries.map(e => {
      //   count++
      //   return {id: count, ...e}
      // })

      // this.logEntries.push(...newEntries)

      // const chunk = document.createElement("DIV")
      // chunk.className = "execution-log__chunk ansicolor-on"
      // const frag = document.createDocumentFragment()
      // frag.appendChild(chunk)

      // newEntries.forEach( e => {
      //   console.log(e)
      //   const span = document.createElement("SPAN")

      //   chunk.append(span)

      //   const vue = new EntryFlex({el: span, propsData: {entry: e}})
      //   vue.$on('line-select', this.handleLineSelect)
      //   this.vues.push(vue)
      // })
      
      // const scroller = this.$refs["scroller"] as HTMLElement
      // scroller.appendChild(frag)

      /**
       * Use Builder
       */
      res.entries.forEach(e => {
        const vue = this.logBuilder.addLine(e)
        this.vues.push(vue)
      })

      if (this.follow) {
        this.scrollToLine(this.vues.length)
      }

      if (this.jumpToLine && this.jumpToLine <= this.vues.length && !this.jumped) {
        this.scrollToLine(this.jumpToLine)
        this.jumped = true
      }
    }

    scrollToLine(n: number | string) {
        const scroller = this.$refs["scroller"] as HTMLElement

        const target = this.vues[Number(n)-1].$el
        const parent = target.parentNode

        scroller.scrollTop = target.offsetTop + parent.offsetTop
    }

    private handleLineSelect(e: any) {
      alert(e)
    }

    private handleJump(e: string) {
        this.scrollToLine(this.jumpToLine || 0)
    }

    private handleJumpToEnd() {
        this.scrollToLine(this.vues.length)
    }

    private handleJumpToStart() {
        this.scrollToLine(1)
    }

    private async populateLogs() {
        while(this.consumeLogs) {
            if (!this.resp)
              this.resp = this.viewer.getOutput(this.batchSize)

            const res = await this.resp
            this.resp = undefined

            if (!this.viewer.completed) {
              this.resp = this.viewer.getOutput(this.batchSize)
              await new Promise((res, rej) => setTimeout(() => {res()},0))
            }

            this.handleExecutionLogResp(res)

            if (this.viewer.completed)
                break
        }
        this.totalTime = Date.now() - this.startTime
        this.populateLogsProm = undefined
    }
}
</script>

<style lang="scss">
@import './ansi.css';

.execution-log--dark .execution-log__gutter {
  background-color: rgb(32, 56, 56);
  color: bisque;
  box-sizing: border-box;
  border-style: none none dotted none;
  border-width: 1px;
  border-color: rgb(22, 36, 36);
}

.execution-log--dark .execution-log__content {
  background-color: darkslategrey;
  color: cornsilk;
  box-sizing: border-box;
  border-style: none none dotted none;
  border-width: 1px;
  border-color: rgb(39, 66, 66);
}

.execution-log--dark .execution-log__node-chunk {
  border-style: none none solid none;
  border-width: 1px;
  border-color: darkolivegreen;
}

.execution-log__node-chunk {
  contain: layout
}

.execution-log__chunk {
  contain: layout;
}

</style>

<style lang="scss" scoped>

.execution-log {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.execution-log__stats {
  flex: 0 0 1em;
}

.execution-log__controls {
    contain: layout;
}

.execution-log__scroller {
  // height: 100%;
  overflow-y: auto;
  will-change: transform;
  width: 100%;
  flex: 1;
}

</style>