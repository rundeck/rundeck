<template>
  <div class="execution-log"
    :class="[`execution-log--${colorTheme()}`]"
  >
    <rd-drawer
        title="Settings"
        placement="left"
        :mask="false"
        :visible="settingsVisible"
        :closable="true"
        :get-container="false"
        :wrap-style="{ position: 'absolute' }"
        @close="() => {settingsVisible = false}"
    >
      <form style="padding: 10px;">
        <div class="form-group">
          <label>Theme</label>
          <select class="form-control select"
            v-model="settings.theme"
          >
            <option v-for="themeOpt in themes" :key="themeOpt.value" :value="themeOpt.value">{{themeOpt.label}}</option>
          </select>
        </div>
        <div class="checkbox">
          <input type="checkbox" v-model="settings.gutter" id="logview_gutter">
          <label for="logview_gutter">Display Gutter</label>
        </div>
        <div class="checkbox">
          <input type="checkbox" v-model="settings.timestamps" id="logview_timestamps">
          <label for="logview_timestamps">Display Timestamps</label>
        </div>
        <div class="checkbox" >
          <input type="checkbox" v-model="settings.command" id="logview_command">
          <label for="logview_command">Display Command</label>
        </div>
        <div class="checkbox">
          <input type="checkbox" v-model="settings.nodeBadge" id="logview_nodeBadge">
          <label for="logview_nodeBadge">Display Node Badge</label>
        </div>
        <div class="checkbox">
          <input type="checkbox" v-model="settings.ansiColor" id="logview_ansiColor">
          <label for="logview_ansiColor">Render ANSI Colors</label>
        </div>
        <div class="checkbox">
          <input type="checkbox" v-model="settings.lineWrap" id="logview_lineWrap">
          <label for="logview_lineWrap">Wrap Long Lines</label>
        </div>
        <div class="checkbox">
          <input type="checkbox" v-model="settings.stats" id="logview_stats">
          <label for="logview_stats">Display Stats</label>
        </div>
      </form>
    </rd-drawer>
    <div 
      ref="scroller"
      class="execution-log__scroller" v-bind:class="{
      'execution-log--no-transition': this.logLines > 1000,
      'ansicolor-on': this.settings.ansiColor
    }">
      <div ref="log">
        <div v-if="showSettings" class="execution-log__settings"  style="margin-left: 5px; margin-right: 5px;">
          <btn-group>
            <btn size="xs" @click="(e) => {settingsVisible = !settingsVisible; e.target.blur();}">
              <i class="fas fa-cog"/>Settings
            </btn>
            <btn size="xs" @click="(e) => {this.follow = !this.follow; e.target.blur();}">
              <i :class="[followIcon]"/>Follow
            </btn>
          </btn-group>
          <transition name="fade">
            <div class="execution-log__progress-bar" v-if="showProgress">
              <progress-bar v-model="barProgress" :type="progressType" :label-text="progressText" label min-width striped active @click="() => {this.consumeLogs = !this.consumeLogs}"/>
            </div>
          </transition>
        </div>
        <div class="execution-log__warning" v-if="overSize">
          <h3> 🐋 {{+(logSize / 1048576).toFixed(2)}}MiB is a whale of a log! 🐋 </h3>
          <h4> Select a download option above to avoid sinking the ship. </h4>
          <h5> Log content may be truncated </h5>
        </div>
        <div class="execution-log__warning" v-if="errorMessage">
          <h4>{{errorMessage}}</h4>
        </div>
        <div class="execution-log__warning" v-if="completed && logLines == 0">
          <h5>No output</h5>
        </div>
      </div>
    </div>
    <div class="stats" v-if="settings.stats">
      <span>Following:{{follow}} Lines:{{logLines.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",")}} Size:{{logSize}}b TotalTime:{{totalTime/1000}}s</span>
    </div>
  </div>
</template>

<script lang="ts">
import {CancellationTokenSource, CancellationToken} from 'prex'

import {ExecutionLog, EnrichedExecutionOutput} from '../../utilities/ExecutionLogConsumer'
import { Component, Prop, Watch, Vue, Inject } from 'vue-property-decorator'
import { ComponentOptions } from 'vue'
import Entry from './logEntry.vue'
import EntryFlex from './logEntryFlex.vue'
import { ExecutionOutputGetResponse } from '@rundeck/client/dist/lib/models'

import {LogBuilder} from './logBuilder'
import VueRouter from 'vue-router'

import { RootStore } from '../../stores/RootStore'
import RdDrawer from '../containers/drawer/Drawer.vue'
import { ExecutionOutput, ExecutionOutputEntry } from '../../stores/ExecutionOutput'
import { Observer } from 'mobx-vue'

const CONFIG_STORAGE_KEY='execution-viewer'

interface IEventViewerSettings {
  theme: string
  stats: boolean
  timestamps: boolean
  command: boolean
  gutter: boolean
  nodeBadge: boolean
  ansiColor: boolean
  lineWrap: boolean
}

@Observer
@Component({
  components: {
    'rd-drawer': RdDrawer,
  }
})
export default class LogViewer extends Vue {
    @Prop()
    executionId!: number

    @Prop()
    node?: string

    @Prop()
    stepCtx?: string

    @Prop({default: true})
    showStats!: boolean

    @Prop({default: true})
    showSettings!: boolean

    @Prop({default: false})
    follow!: boolean

    @Prop()
    jumpToLine?: number

    @Prop({default: 'dark'})
    theme?: string

    @Prop({default: 3145728})
    maxLogSize!: number

    @Prop()
    trimOutput!: number

    @Prop()
    config?: IEventViewerSettings

    @Prop({default: true})
    useUserSettings!: boolean

    @Inject({default: undefined})
    private readonly executionLogViewerFactory?: (execId: string) => Promise<ExecutionLog>

    @Inject()
    private readonly rootStore!: RootStore

    themes = [
      {label: 'Rundeck Theme', value: 'rundeck'},
      {label: 'Light', value: 'light'},
      {label: 'Dark', value: 'dark'},
      {label: 'None', value: 'none'}
    ]

    scrollTolerance = 5

    batchSize = 200

    totalTime = 0

    progress = 0

    private settings: IEventViewerSettings = {
      theme: 'rundeck',
      stats: false,
      timestamps: false,
      command: true,
      gutter: true,
      ansiColor: true,
      nodeBadge: true,
      lineWrap: true
    }

    @Watch('settings', {deep: true})
    private handleUserConfigChange(newVal: any, oldVal: any) {
      if (this.useUserSettings && this.settingsVisible)
        this.saveConfig()
    }

    @Watch('settingsChange', {deep: true})
    private handleSettingsChange(newVal: IEventViewerSettings, oldVal: IEventViewerSettings) {
      const updatableProps: Array<keyof IEventViewerSettings> = ['nodeBadge', 'gutter', 'timestamps', 'lineWrap', 'command']

      for (const prop of updatableProps) {
        if (newVal[prop] != oldVal[prop]) {
          this.$options.vues.forEach(v => v[prop] = newVal[prop])
        }
      }

      this.logBuilder.updateProp({
        node: this.node,
        stepCtx: this.stepCtx,
        nodeIcon: this.settings.nodeBadge,
        maxLines: 20000,
        command: {
          visible: this.settings.command
        },
        time: {
          visible: this.settings.timestamps
        },
        gutter: {
          visible: this.settings.gutter
        },
        content: {
          lineWrap: this.settings.lineWrap
        }
      })
    }

    private consumeLogs: boolean = true

    private completed = false

    private errorMessage = ''

    private execCompleted = true

    private settingsVisible = false

    private overSize = false

    private jumped = false

    private viewer!: ExecutionOutput

    private logBuilder!: LogBuilder

    private logEntries: Array<{log?: string, id: number}> = []

    private scrollCount = 0

    private startTime = 0

    private logSize = 0

    private logLines = 0

    private resp?: Promise<ExecutionOutputEntry[]>

    private populateLogsProm?: Promise<void>

    private cancelProgress?: CancellationTokenSource

    private nextProgress = 0

    private selected: any

    private percentLoaded: number = 0

    $options!: ComponentOptions<Vue> & {
      vues: any[]
    }

    get settingsChange() {
      return Object.assign({}, this.settings)
    }

    get followIcon() {
      return this.follow ? 'fas fa-eye' : 'fas fa-eye-slash'
    }

    get barProgress() {
      return this.execCompleted ? this.progress : 100
    }

    get progressType() {
      return this.consumeLogs ? 'info' : 'warning'
    }

    get progressText() {
      const loadingText = `${this.barProgress}% ${this.consumeLogs ? 'Pause' : 'Resume'}`
      const runningText = `${this.consumeLogs ? 'Pause' : 'Resume'}`

      return this.execCompleted ? loadingText : runningText
    }

    get showProgress(): boolean {
      return (!this.completed || !this.execCompleted)
    }

    @Watch('logSize')
    checkForOversize(val: number, oldVal: number) {
      if (val > this.maxLogSize) {
        this.overSize = true
        this.nextProgress = 100
        this.completed = true
      }
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

    created() {
      /** Load here so theme does not change afer visible */
      this.loadConfig()
    }

    async mounted() {
        this.$options.vues = []

        const scroller = this.$refs["scroller"] as HTMLElement
        const log = this.$refs["log"] as HTMLElement

        this.viewer = this.rootStore.executionOutputStore.createOrGet(this.executionId.toString())

        this.logBuilder = new LogBuilder(this.viewer, log, {
          node: this.node,
          stepCtx: this.stepCtx,
          nodeIcon: this.settings.nodeBadge,
          maxLines: 20000,
          command: {
            visible: this.settings.command
          },
          time: {
            visible: this.settings.timestamps
          },
          gutter: {
            visible: this.settings.gutter
          },
          content: {
            lineWrap: this.settings.lineWrap
          }
        })

        this.logBuilder.onNewLines(this.handleNewLine)

        this.startTime = Date.now()
        this.addScrollBlocker()

        this.updateProgress()

        const {viewer} = this
        await viewer.init()

        this.execCompleted = viewer.execCompleted
        this.follow = !viewer.execCompleted

        if (viewer.execCompleted && viewer.size > this.maxLogSize) {
          this.logSize = viewer.size
          this.nextProgress = 0
          this.updateProgress(100)
          return
        }

        this.populateLogsProm = this.populateLogs()
    }

    private loadConfig() {
      if (this.useUserSettings) {
        const settings = localStorage.getItem(CONFIG_STORAGE_KEY)

        if (settings) {
          try {
            const config = JSON.parse(settings)
            Object.assign(this.settings, config)
          } catch (e) {
            localStorage.removeItem(CONFIG_STORAGE_KEY)
          }
        }
      }

      Object.assign(this.settings, this.config || {})
    }

    private saveConfig() {
      localStorage.setItem(CONFIG_STORAGE_KEY, JSON.stringify(this.settings))
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

    private handleExecutionLogResp() {
      if (!this.trimOutput) return

      if (this.viewer.offset > this.trimOutput) {
        const removeSize = this.logBuilder.dropChunk()
        for (let x = 0; x < removeSize; x++) {
          const scapeGoat = this.$options.vues.shift()
        }
      }
    }

    private updateProgress(delay: number = 0) {
      if (this.cancelProgress)
        this.cancelProgress.cancel()

      this.cancelProgress = new CancellationTokenSource();

      (async (cancel: CancellationToken) => {
        const update = () => {this.progress = this.nextProgress}
        setTimeout(update, delay)
        while(!cancel.cancellationRequested) {
          await new Promise((res, rej) => {setTimeout(res, 1000)})
          if (this.progress == 100)
            this.cancelProgress?.cancel()
          update()
        }
      })(this.cancelProgress.token)
    }

    scrollToLine(n: number | string) {
        const scroller = this.$refs["scroller"] as HTMLElement

        const target = this.$options.vues[Number(n)-1].$el
        let parent = target.parentNode

        let offset = target.offsetTop

        // Traverse to root and accumulate offset
        while (parent != scroller) {
          offset += parent.offsetTop
          parent = parent.parentNode
        }

        scroller.scrollTop = offset - 24 // Insure under stick header
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

      const line = this.$options.vues[e-1]

      if (this.selected) {
        this.selected.selected = false
      }

      if (this.selected === line) {
        this.selected = undefined
        this.$emit('line-deselect', e)
        return
      }

      line.selected = true
      this.selected = line

      this.$emit('line-select', e)
    }

    private handleNewLine(entries: Array<any>) {
      for (const vue of entries) {
        // @ts-ignore
        const selected = vue.$options.entry.lineNumber == this.jumpToLine
        vue.$on('line-select', this.handleLineSelect)
        if (selected) {
          this.selected = vue
          vue.selected = true
        }
      }

      this.$options.vues.push(...entries)

      if (this.jumpToLine && this.jumpToLine <= this.$options.vues.length && !this.jumped) {
        this.follow = false
        this.scrollToLine(this.jumpToLine)
        this.jumped = true
      }

      if (this.follow) {
        this.scrollToLine(this.$options.vues.length)
      }
    }

    private handleJump(e: string) {
        this.scrollToLine(this.jumpToLine || 0)
    }

    private handleJumpToEnd() {
        this.scrollToLine(this.$options.vues.length)
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
              await new Promise<void>((res, rej) => setTimeout(() => {res()},0))
            }

            this.execCompleted = this.viewer.execCompleted
            this.completed = this.viewer.completed

            this.nextProgress = Math.round(this.viewer.percentLoaded)

            if (this.viewer.error)
              this.errorMessage = this.viewer.error

            this.logSize = this.viewer.offset
            this.logLines = this.viewer.entries.length
            this.handleExecutionLogResp()

            if (this.viewer.completed)
                break
        }
        this.totalTime = Date.now() - this.startTime
        this.populateLogsProm = undefined
    }

    colorTheme() {
      if (this.settings.theme == 'rundeck')
        return this.rootStore.theme.theme
      else
        return this.settings.theme

    }
}
</script>

<style lang="scss">
@import './ansi.css';
@import './theme-light.scss';
@import './theme-dark.scss';

.anticon {
  display: inline-block;
  color: inherit;
  font-style: normal;
  line-height: 0;
  text-align: center;
  text-transform: none;
  vertical-align: -0.125em;
  text-rendering: optimizeLegibility;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

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

.progress{
  height: 20px;
  margin:0;
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
  top: -2px;
  z-index: 1;
  transition: top .3s;

  .btn i {
    margin-right: 5px;
  }
}

.execution-log__settings:hover, .execution-log__settings:focus-within {
  top: -2px;
}

.execution-log__stats {
  flex: 0 0 1em;
}

.execution-log__scroller {
  // height: 100%;
  overflow-y: auto;
  /** Causing FireFox scroll issues: translate3d(0,0,0) fixes but conflicts with modals */
  // will-change: transform;
  width: 100%;
  flex: 1;
}

.execution-log__warning {
  width: 100%;
  text-align: center;
}

</style>
