<template>
  <div class="execution-log"
    v-bind:class="{
      'execution-log--dark': this.settings.theme == 'dark',
      'execution-log--light': this.settings.theme == 'light',
      }"
  >
    <a-drawer
        title="Settings"
        placement="left"
        :mask="false"
        :visible="settingsVisible"
        :closable="true"
        :get-container="false"
        :wrap-style="{ position: 'absolute' }"
        @close="() => {settingsVisible = false}"
    >
      <a-form>
        <a-form-item label="Theme">
          <a-radio-group v-model="settings.theme" size="small">
            <a-radio-button v-for="themeOpt in themes" :key="themeOpt" :value="themeOpt">
              {{themeOpt}}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="Display Gutter">
          <a-switch v-model="settings.gutter"/>
        </a-form-item>
        <a-form-item label="Display Timestamps">
          <a-switch v-model="settings.timestamps"/>
        </a-form-item>
        <a-form-item label="Display Stats">
          <a-switch v-model="settings.stats"/>
        </a-form-item>
      </a-form>
    </a-drawer>
    <div ref="scroller" class="execution-log__scroller" v-bind:class="{'execution-log--no-transition': this.vues.length > 1000}">
      <div class="execution-log__settings"  style="margin-left: 5px; margin-right: 5px;">
        <a-button-group>
          <a-tooltip title="Settings">
            <a-button size="small" @click="(e) => {settingsVisible = !settingsVisible; e.target.blur();}" icon="setting"></a-button>
          </a-tooltip>
          <a-tooltip title="Follow">
            <a-button size="small" :icon="followIcon" @click="(e) => {this.follow = !this.follow; e.target.blur();}"></a-button>
          </a-tooltip>
        </a-button-group>
        <transition name="fade">
          <div class="execution-log__progress-bar" v-if="showProgress">
            <progress-bar v-model="barProgress" :type="progressType" :label-text="progressText" label min-width striped active @click="() => {this.consumeLogs = !this.consumeLogs}"/>
          </div>
        </transition>
      </div>
        <div class="execution-log__size-warning" v-if="overSize">
          <h3> 🐋 {{+(logSize / 1048576).toFixed(2)}}MiB is a whale of a log! 🐋 </h3>
          <h4> Select a download option above to avoid sinking the ship. </h4>
          <h5> Log content may be truncated </h5>
        </div>
    </div>
    <div class="stats" v-if="settings.stats">
      <span>Following:{{follow}} Lines:{{vues.length.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",")}} Size:{{logSize}}b TotalTime:{{totalTime/1000}}s</span>
    </div>
  </div>
</template>

<script lang="ts">
import {Button, Drawer, Form, Radio, Select, Switch, Tooltip} from 'ant-design-vue'

import {CancellationTokenSource, CancellationToken} from 'prex'

import {ExecutionLog, EnrichedExecutionOutput} from '@/utilities/ExecutionLogConsumer'
import { Component, Prop, Watch, Vue } from 'vue-property-decorator'
import Entry from './logEntry.vue'
import EntryFlex from './logEntryFlex.vue'
import { ExecutionOutputGetResponse } from 'ts-rundeck/dist/lib/models'

import {LogBuilder} from './logBuilder'
import VueRouter from 'vue-router'

Vue.use(Tooltip)

@Component({
  components: {
    'a-select': Select,
    'a-switch': Switch,
    'a-button': Button,
    'a-button-group': Button.Group,
    'a-drawer': Drawer,
    'a-form': Form,
    'a-form-item': Form.Item,
    'a-radio': Radio,
    'a-radio-group': Radio.Group,
    'a-radio-button': Radio.Button,
  }
})
export default class LogViewer extends Vue {
    @Prop()
    executionId!: number

    @Prop({default: true})
    showStats!: boolean

    @Prop({default: false})
    follow!: boolean

    @Prop()
    jumpToLine?: number

    @Prop({default: 'dark'})
    theme?: string

    @Prop({default: 3145728})
    maxLogSize!: number

    themes = ['light', 'dark', 'none']

    scrollTolerance = 5

    batchSize = 200

    totalTime = 0

    progress = 0

    settings = {
      theme: 'light',
      stats: true,
      timestamps: false,
      gutter: true,
    }

    @Watch('settings', {deep: true})
    private handleUserConfigChange(oldVal: any, newVal: any) {
      this.saveConfig()
    }

    private consumeLogs: boolean = true

    private completed = false

    private execCompleted = true

    private settingsVisible = false

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

    get followIcon() {
      return this.follow ? 'eye' : 'eye-invisible'
    }

    get barProgress() {
      return this.execCompleted ? this.progress : 100
    }

    get progressType() {
      return this.consumeLogs ? 'info' : 'warning'
    }

    get progressText() {
      const loadingText = `${this.progress}% ${this.consumeLogs ? 'Pause' : 'Resume'}`
      const runningText = `${this.consumeLogs ? 'Pause' : 'Resume'}`

      return this.execCompleted ? loadingText : runningText
    }

    get showProgress(): boolean {
      return (!this.completed || !this.execCompleted)
    }

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
        this.loadConfig()

        const scroller = this.$refs["scroller"] as HTMLElement
        this.viewer = new ExecutionLog(this.executionId.toString())
        this.logBuilder = new LogBuilder(scroller, {
          nodeIcon: true,
          maxLines: 20000,
          time: {
            visible: this.settings.timestamps
          },
          gutter: {
            visible: this.settings.gutter
          }
        })

        this.startTime = Date.now()
        this.addScrollBlocker()

        this.updateProgress()

        const {viewer} = this
        await viewer.init()

        this.execCompleted = viewer.execCompleted
        this.follow = !viewer.execCompleted

        if (viewer.execCompleted && viewer.size > this.maxLogSize) {
          this.logSize = viewer.size
          this.nextProgress = -1
          this.updateProgress(100)
          return
        }

        this.populateLogsProm = this.populateLogs()
    }

    private loadConfig() {
      const settings = localStorage.getItem('execution-viewer-beta')

      if (settings) {
        try {
          const config = JSON.parse(settings)
          Object.assign(this.settings, config)
        } catch (e) {
          localStorage.removeItem('execution-viewer-beta')
        }
      }
    }

    private saveConfig() {
      localStorage.setItem('execution-viewer-beta', JSON.stringify(this.settings))
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
      this.execCompleted = res.execCompleted
      this.completed = res.completed
      this.nextProgress = res.completed ?
        100:
        Math.round((parseInt(res.offset) / res.totalSize) * 100)

      if (res.entries.length == 0)
        return

      if (this.overSize && this.viewer.offset > this.maxLogSize) {
        const removeSize = this.logBuilder.dropChunk()
        for (let x = 0; x < removeSize; x++) {
          const scapeGoat = this.vues.shift()
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

    private updateProgress(delay: number = 0) {
      if (this.cancelProgress)
        this.cancelProgress.cancel()
      
      this.cancelProgress = new CancellationTokenSource();

      (async (cancel: CancellationToken) => {
        const update = () => {this.progress = this.nextProgress}
        setTimeout(update, delay)
        while(!cancel.cancellationRequested && this.progress > -1) {
          await new Promise((res, rej) => {setTimeout(res, 1000)})
          if (this.progress == 100)
            this.nextProgress = -1
          update()
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

    /**
     * Handle line select events from the log entries and re-emit.
     * Emits a line-deselect if the event is for the currently selected line.
     */
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

/** Causing z-index issues with modals */
// .main-panel {
//   transform: translate3d(0,0,0)
// }

.execution-log__progress-bar {
  display: inline-block;
  height: min-content;
  flex: 1;
  cursor: pointer;
}

.execution-log__progress-bar * {
  transition: all .3s ease;
}

.fade-enter-active, .fade-leave-active {
  transition: opacity .5s;
}
.fade-enter, .fade-leave-to * {
  opacity: 0;
}

.execution-log--no-transition * {
  transition: none !important;
}

.execution-log__node-chunk * {
  transition: all 0.3s ease;
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
  position: relative;
  overflow: hidden;
}

.execution-log__settings {
  display: flex;
  align-items: center;
  position: sticky;
  top: -15px;
  z-index: 1;
  transition: top .3s;
}

.execution-log__settings:hover, .execution-log__settings:focus-within {
  top: -2px;
}

.execution-log__stats {
  flex: 0 0 1em;
}

.execution-log__controls {
    contain: layout;
    display: flex;
}

.execution-log__scroller {
  // height: 100%;
  overflow-y: auto;
  /** Causing FireFox scroll issues: translate3d(0,0,0) fixes but conflicts with modals */
  // will-change: transform;
  width: 100%;
  flex: 1;
}

.execution-log__size-warning {
  width: 100%;
  text-align: center;
}

</style>