<template>
  <div class="execution-log">
    <div class="controls">
      Consume:<input type="checkbox" v-model="consumeLogs"
      />&nbsp;&nbsp;&nbsp;&nbsp;Stats:<input type="checkbox" v-model="showStats"
      >&nbsp;&nbsp;&nbsp;&nbsp;Follow:<input type="checkbox" v-model="follow"
      />&nbsp;&nbsp;&nbsp;&nbsp;Jump:<input v-model="jumpToLine" v-on:keydown.enter="handleJump">
    </div>
    <div class="stats" v-if="showStats">
      <span>Following:{{follow}} Lines:{{logEntries.length.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",")}} TotalTime:{{totalTime/1000}}s</span>
    </div>
    <div ref="scroller" class="scroller"/>
  </div>
</template>

<script lang="ts">
import {ExecutionLog} from '@/utilities/ExecutionLogConsumer'
import { Component, Prop, Watch, Vue } from 'vue-property-decorator'
import { RecycleScroller, DynamicScroller, DynamicScrollerItem } from 'vue-virtual-scroller'
import Entry from './logEntry.vue'
import EntryFlex from './logEntryFlex.vue'
import { ExecutionOutputGetResponse } from 'ts-rundeck/dist/lib/models';

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

    scrollTolerance = 5

    totalTime = 0

    private jumped = false

    private viewer!: ExecutionLog

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
        this.viewer = new ExecutionLog(this.executionId.toString())
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
      let count = this.logEntries.length

      const newEntries = res.entries.map(e => {
        count++
        return {id: count, ...e}
      })

      this.logEntries.push(...newEntries)

      const chunk = document.createElement("DIV")
      chunk.className = "log-chunk ansicolor-on"
      const frag = document.createDocumentFragment()
      frag.appendChild(chunk)

      newEntries.forEach( e => {
        const span = document.createElement("SPAN")

        chunk.append(span)

        const vue = new EntryFlex({el: span, propsData: {entry: e}})
        vue.$on('line-select', this.handleLineSelect)
        this.vues.push(vue)
      })
      
      const scroller = this.$refs["scroller"] as HTMLElement
      scroller.appendChild(frag)

      if (this.follow) {
          const last = scroller.lastChild!.lastChild as HTMLElement
          if (last && this.follow) {
            console.log('Scroll')
            last.scrollIntoView()
          }
      }

        if (this.jumpToLine && this.jumpToLine <= this.vues.length && !this.jumped) {
            this.scrollToLine(this.jumpToLine)
            this.jumped = true
        }
    }

    scrollToLine(n: number | string) {
        console.log(`Scroll to ${n}`)
        const scroller = this.$refs["scroller"] as HTMLElement
        this.vues[Number(n)-1].$el.scrollIntoView()
    }

    private handleLineSelect(e: any) {
      alert(e)
    }

    private handleJump(e: string) {
        this.scrollToLine(this.jumpToLine || 0)
    }

    private async populateLogs() {
        while(this.consumeLogs) {
            if (!this.resp)
              this.resp = this.viewer.getOutput(500)

            const res = await this.resp
            this.resp = undefined

            if (!this.viewer.completed) {
              this.resp = this.viewer.getOutput(500)
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

.log-gutter {
  background-color: rgb(32, 56, 56);
  color: cornsilk;
  box-sizing: border-box;
  border-style: none none dotted none;
  border-width: 1px;
  border-color: rgb(22, 36, 36);
}

.log-content {
  background-color: darkslategrey;
  color: bisque;
  box-sizing: border-box;
  border-style: none none dotted none;
  border-width: 1px;
  border-color: rgb(39, 66, 66);
}

.log-chunk {
  contain: layout;
}

</style>

<style lang="scss" scoped>

.execution-log {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.stats {
  flex: 0 0 1em;
}

.controls {
    contain: layout;
}

.scroller {
  // height: 100%;
  overflow-y: auto;
  will-change: transform;
  width: 100%;
  flex: 1;
}

</style>