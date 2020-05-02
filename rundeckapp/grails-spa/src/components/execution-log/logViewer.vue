<template>
  <div class="execution-log"
    v-if="this.$el.parentNode.display != 'none'"
    v-bind:class="{
      'execution-log--dark': this.theme == 'dark',
      'execution-log--light': this.theme == 'light',
      'execution-log--no-transition': this.vues.length > 1000,
      }"
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
    <div v-if="progress > -1 && progress != 100">
      <progress-bar v-model="progress" type="info" label min-width striped active>
    </div>
    <div class="stats" v-if="showStats">
      <span>Following:{{follow}} Lines:{{vues.length.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",")}} Size:{{logSize}}b TotalTime:{{totalTime/1000}}s</span>
    </div>
    <div class="execution-log__size-warning" v-if="overSize">
      <h3> 🐋 {{+(logSize / 1048576).toFixed(2)}}MiB is a whale of a log! 🐋 </h3>
      <h4> Select a download option above to avoid sinking the ship. </h4>
    </div>
    <div ref="scroller" class="execution-log__scroller"/>
  </div>
</template>

<script lang="ts">
import {CancellationTokenSource, CancellationToken} from 'prex'

import {ExecutionLog, EnrichedExecutionOutput} from '@/utilities/ExecutionLogConsumer'
import { Component, Prop, Watch, Vue } from 'vue-property-decorator'
import Entry from './logEntry.vue'
import EntryFlex from './logEntryFlex.vue'
import { ExecutionOutputGetResponse } from 'ts-rundeck/dist/lib/models'

import {LogBuilder} from './logBuilder'
import VueRouter from 'vue-router'

@Component
export default class LogViewer extends Vue {
    @Prop()
    executionId!: number

    @Prop()
    consumeLogs: boolean = true

    @Prop()
    showStats = true

    @Prop({default: false})
    follow!: boolean

    @Prop()
    jumpToLine?: number

    @Prop({default: 'dark'})
    theme?: string

    @Prop({default: 1048576})
    maxLogSize!: number

    scrollTolerance = 5

    batchSize = 200

    totalTime = 0

    progress = -1

    private overSize = false

    private jumped = false

    private viewer!: ExecutionLog

    private logBuilder!: LogBuilder

    private logEntries: Array<{log?: string, id: number}> = []

    private scrollCount = 0

    private startTime = 0

    private logSize = 0

    private resp?: Promise<EnrichedExecutionOutput>

    private populateLogsProm?: Promise<void>

    private cancelProgress?: CancellationTokenSource

    private nextProgress = 0

    private vues: any[] = []

    private selected: any

    @Watch('logSize')
    checkForOversize(val: number, oldVal: number) {
      if (val > this.maxLogSize)
        this.overSize = true
    }

    @Watch('consumeLogs')
    toggleConsumeLogs(val: boolean, oldVal: boolean) {
      if(val)
        this.updateProgress()
      else if(this.cancelProgress)
        this.cancelProgress.cancel()

      if(val && !this.populateLogsProm) {
        this.populateLogsProm = this.populateLogs()
      }
    }

    async mounted() {
        const scroller = this.$refs["scroller"] as HTMLElement
        this.viewer = new ExecutionLog(this.executionId.toString())
        this.logBuilder = new LogBuilder(scroller, {nodeIcon: true})
        
        const {viewer} = this
        await viewer.init()
        if (viewer.execCompleted && viewer.size > this.maxLogSize) {
          this.logSize = viewer.size
          return
        }

        this.startTime = Date.now()
        this.addScrollBlocker()
        this.progress = 0
        this.updateProgress()
        this.populateLogsProm = this.populateLogs()
    }

    /**
     * Allows us to prevent scrolling unless a certain amount of "resistence" is produced
     */
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

    private handleExecutionLogResp(res: EnrichedExecutionOutput) {
      
      this.nextProgress = Math.round((parseInt(res.offset) / res.totalSize) * 100)

      if (this.overSize && this.viewer.offset > this.maxLogSize) {
        console.log('over')
        for (let x = 0; x < res.entries.length; x++) {
          const scapeGoat = this.vues.shift()
          scapeGoat.$el.remove()
        }
      }

      res.entries.forEach(e => {
        const selected = e.lineNumber == this.jumpToLine
        const vue = this.logBuilder.addLine(e, selected)
        vue.$on('line-select', this.handleLineSelect)
        this.vues.push(vue)
        if (selected)
          this.selected = vue
      })

      if (this.jumpToLine && this.jumpToLine <= this.vues.length && !this.jumped) {
        this.follow = false
        this.scrollToLine(this.jumpToLine)
        this.jumped = true
      }

      if (this.follow) {
        this.scrollToLine(this.vues.length)
      }
    }

    private updateProgress() {
      if (this.cancelProgress)
        this.cancelProgress.cancel()
      
      this.cancelProgress = new CancellationTokenSource();

      (async (cancel: CancellationToken) => {
        while(!cancel.cancellationRequested && this.progress != 100) {
          await new Promise((res, rej) => {
            setInterval(() => this.progress = this.nextProgress, 2000)
          })
        }
      })(this.cancelProgress.token)
    }

    scrollToLine(n: number | string) {
        const scroller = this.$refs["scroller"] as HTMLElement

        const target = this.vues[Number(n)-1].$el
        let parent = target.parentNode

        let offset = target.offsetTop

        // Traverse to root and accumulate offset
        while (parent != scroller) {
          offset += parent.offsetTop
          parent = parent.parentNode
        }

        scroller.scrollTop = offset
    }

    private handleLineSelect(e: any) {
      if (this.overSize) {
        alert('Line-linking is not supported for over-sized logs')
        return
      }

      const line = this.vues[e-1]

      if (this.selected)
        this.selected.selected = false

      if (this.selected === line) {
        this.selected = undefined
        this.$emit('line-deselect', e)
        return
      }

      line.selected = true
      this.selected = line

      this.$emit('line-select', e)
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
              this.resp = this.viewer.getEnrichedOutput(this.batchSize)
            const res = await this.resp
            this.resp = undefined

            if (!this.viewer.completed) {
              this.resp = this.viewer.getEnrichedOutput(this.batchSize)
              await new Promise((res, rej) => setTimeout(() => {res()},0))
            }

            this.logSize = this.viewer.offset
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
@import './theme-light.scss';
@import './theme-dark.scss';

.execution-log--no-transition * {
  transition: none !important;
}

.execution-log * {
  transition: all .3s ease;
}

.execution-log__node-chunk {
  contain: layout;
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

.execution-log__size-warning {
  width: 100%;
  text-align: center;
}

</style>