<template>
  <div class="execution-log" ref="root"
    :class="[`execution-log--${colorTheme}`]"
  >
    <RdDrawer
        title="Settings"
        placement="left"
        :mask="false"
        :visible="settingsVisible"
        :closable="true"
        :get-container="false"
        :wrap-style="{ position: 'absolute' }"
        @close="closeSettings"
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
        <div class="vue-ui-socket">
          <ui-socket section="execution-log-viewer" location="settings"
                     :event-bus="eventBus"
          />
        </div>
      </form>
    </RdDrawer>
    <div 
      ref="scroller"
      class="execution-log__scroller"
      :class="{
        'execution-log--no-transition': logLines > 1000,
        'ansicolor-on': settings.ansiColor
      }"
    >
      <div ref="log" class="execution-log__scroller-item-container">
        <div v-if="showSettings" class="execution-log__settings"  style="margin-left: 5px; margin-right: 5px;">
          <btn-group>
            <btn size="xs" @click="toggleSettings">
              <i class="fas fa-cog"/>Settings
            </btn>
            <btn size="xs" @click="toggleFollow">
              <i :class="[followIcon]"/>Follow
            </btn>
          </btn-group>
          <transition name="fade">
            <div class="execution-log__progress-bar" v-if="showProgress">
              <progress-bar v-model="barProgress" :type="progressType" :label-text="progressText" label min-width striped active @click="toggleProgressBar"/>
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
        <div class="execution-log__warning" v-if="completed && logLines === 0">
          <h5>No output</h5>
        </div>
        <LogNodeChunk ref="logEntryChunk"
                      class="execution-log__node-chunk"
                      :event-bus="eventBus"
                      :selected-line="selectedLineIdx"
                      :node="node"
                      :step-ctx="stepCtx"
                      :node-icon="settings.nodeBadge"
                      :max-line="2000"
                      :command="settings.command"
                      :time="settings.timestamps"
                      :gutter="settings.gutter"
                      :line-wrap="settings.lineWrap"
                      :entries="viewerEntries"
                      :jump-to-line="jumpToLine"
                      :jumped="jumped"
                      @line-select="handleLineSelect"
                      @jumped="jumped = true"
        />
      </div>
    </div>
    <div class="stats" v-if="settings.stats">
      <span>Following:{{mfollow}} Lines:{{logLines.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",")}} Size:{{logSize}}b TotalTime:{{totalTime/1000}}s</span>
    </div>
  </div>
</template>

<script lang="ts">
import {CancelToken} from '@esfx/canceltoken'

import { defineComponent } from 'vue'
import RdDrawer from '../containers/drawer/Drawer.vue'
import UiSocket from "../utils/UiSocket.vue";
import { EventBus } from '../../utilities/vueEventBus'
import { Btn, BtnGroup, ProgressBar } from 'uiv'
import {App, PropType} from "vue";
import LogNodeChunk from "./LogNodeChunk.vue";
import {JobWorkflow} from "@/library/utilities/JobWorkflow";

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

export default defineComponent({
  name: "LogViewer",
  components: {
    LogNodeChunk,
    UiSocket,
    RdDrawer,
    Btn,
    BtnGroup,
    ProgressBar,
  },
  props: {
    executionId: {
      type: String,
      required: true
    },
    node: {
      type: String,
      required: false
    },
    stepCtx: {
      type: String,
      required: false
    },
    showStats: {
      type: Boolean,
      required: false,
      default: true
    },
    showSettings: {
      type: Boolean,
      required: false,
      default: true
    },
    follow: {
      type: Boolean,
      required: false,
      default: false
    },
    jumpToLine: {
      type: Number,
      required: false
    },
    theme: {
      type: String,
      required: false,
      default: 'dark'
    },
    maxLogSize: {
      type: Number,
      required: false,
      default: 3145728
    },
    trimOutput: {
      type: Number,
      required: false
    },
    config: {
      type: Object as PropType<IEventViewerSettings>,
      required: false
    },
    useUserSettings: {
      type: Boolean,
      required: false,
      default: true
    },
  },
  emits: ['line-select', 'line-deselect'],
  data() {
    return {
      rootStore: window._rundeck.rootStore,
      eventBus: window._rundeck.eventBus as typeof EventBus,
      themes: [
        {label: 'Rundeck Theme', value: 'rundeck'},
        {label: 'Light', value: 'light'},
        {label: 'Dark', value: 'dark'},
        {label: 'None', value: 'none'}
      ],
      settings: {
        theme: 'rundeck',
        stats: false,
        timestamps: false,
        command: true,
        gutter: true,
        ansiColor: true,
        nodeBadge: true,
        lineWrap: true
      },
      consumeLogs: true,
      completed: false,
      errorMessage: '',
      execCompleted: true,
      settingsVisible: false,
      overSize: false,
      jumped: false,
      mfollow: this.follow,
      viewer: null,
      logBuilder: null,
      logEntries: [],
      scrollCount: 0,
      startTime: 0,
      logSize: 0,
      logLines: 0,
      resp: null,
      populateLogsProm: null,
      cancelProgress: null,
      nextProgress: 0,
      scrollTolerance: 5,
      batchSize: 200,
      totalTime: 0,
      progress: 0,
      selected: null,
      selectedLineIdx: null,
      percentLoaded: 0,
      entries: []
    }
  },
  computed: {
    settingsChange() {
      return Object.assign({}, this.settings)
    },
    followIcon() {
      return this.mfollow ? 'fas fa-eye' : 'fas fa-eye-slash'
    },
    barProgress() {
      return this.execCompleted ? this.progress : 100
    },
    progressType() {
      return this.consumeLogs ? 'info' : 'warning'
    },
    progressText() {
      const loadingText = `${this.barProgress}% ${this.consumeLogs ? 'Pause' : 'Resume'}`
      const runningText = `${this.consumeLogs ? 'Pause' : 'Resume'}`

      return this.execCompleted ? loadingText : runningText
    },
    showProgress() {
      return (!this.completed || !this.execCompleted)
    },
    colorTheme() {
      if (this.settings.theme == 'rundeck') {
        return this.rootStore.theme.theme
      }
      return this.settings.theme
    },
    viewerEntriesByNodeCtx() {
      return this.viewer.entries.reduce((acc, entry) => ({
        ...acc,
        [`${entry.node}:${entry.stepctx ? JobWorkflow.cleanContextId(entry.stepctx) : ''}`]: [...(acc[`${entry.node}:${entry.stepctx ? JobWorkflow.cleanContextId(entry.stepctx) : ''}`] || []), entry]
      }), {})
    },
    viewerEntriesByNode() {
      return this.viewer.entries.reduce((acc, entry) => {
        return {
          ...acc,
          [entry.node]: [...(acc[entry.node] || []), entry]
        }
      }, {})
    },
    viewerEntries() {
      if (this.viewer == null) {
        return []
      }
      if(this.node){
        if (this.stepCtx) {
          return this.viewerEntriesByNodeCtx[`${this.node}:${JobWorkflow.cleanContextId(this.stepCtx)}`]
        }
        return this.viewerEntriesByNode[this.node]
      }
      return this.viewer.entries
    },
  },
  watch: {
    settings: {
      handler() {
        if (this.useUserSettings && this.settingsVisible) {
          this.saveConfig()
        }
      },
      deep: true
    },
    logSize(val: number) {
      if (val > this.maxLogSize) {
        this.overSize = true
        this.nextProgress = 100
        this.completed = true
      }
    },
    consumeLogs(val: boolean) {
      if (val)
        this.updateProgress()
      else if (this.cancelProgress)
        this.cancelProgress.cancel()

      if (val && !this.populateLogsProm) {
        this.populateLogsProm = this.populateLogs()
      }
    },
  },
  beforeMount() {
    this.loadConfig()
  },
  async mounted() {
    this.viewer = this.rootStore.executionOutputStore.createOrGet(this.executionId)

    this.startTime = Date.now()
    this.addScrollBlocker()

    this.updateProgress()

    await this.viewer.init()

    this.execCompleted = this.viewer.execCompleted
    this.mfollow = !this.viewer.execCompleted

    if (this.viewer.execCompleted && this.viewer.size > this.maxLogSize) {
      this.logSize = this.viewer.size
      this.nextProgress = 0
      this.updateProgress(100)
      return
    }

    this.populateLogsProm = await this.populateLogs()
  },
  methods: {
    loadConfig() {
      if (this.useUserSettings) {
        const _settings = localStorage.getItem(CONFIG_STORAGE_KEY)

        if (_settings) {
          try {
            const config = JSON.parse(_settings)
            Object.assign(this.settings, config)
          } catch (e) {
            localStorage.removeItem(CONFIG_STORAGE_KEY)
          }
        }
      }

      Object.assign(this.settings, this.config || {})
    },
    saveConfig() {
      localStorage.setItem(CONFIG_STORAGE_KEY, JSON.stringify(this.settings))
    },
    addScrollBlocker() {
      /**
       * Allows us to prevent scrolling unless a certain amount of "resistence" is produced
       */
      const _scroller = this.$refs['scroller'] as HTMLElement
      _scroller.addEventListener('wheel', (ev: UIEvent) => {
        this.scrollCount++

        if (this.mfollow) {
          ev.preventDefault()
          ev.returnValue = false
        }

        if (this.scrollCount > this.scrollTolerance)
          this.mfollow = false
      }, {passive: false})
    },
    handleExecutionLogResp() {
      if (!this.trimOutput) return
    },
    updateProgress(delay: number = 0) {
      if (this.cancelProgress)
        this.cancelProgress.cancel()

      this.cancelProgress = CancelToken.source();

      (async (cancel: CancelToken) => {
        const update = () => {
          this.progress = this.nextProgress
        }
        setTimeout(update, delay)
        while (!cancel.signaled) {
          await new Promise((res, rej) => {
            setTimeout(res, 1000)
          })
          if (this.progress == 100)
            this.cancelProgress?.cancel()
          update()
        }
      })(this.cancelProgress.token)
    },
    scrollToLine(n: number | string) {
      const _scroller = this.$refs['scroller'] as HTMLElement
      if(this.$refs["logEntryChunk"]){
        this.$refs['logEntryChunk'].scrollToLine(Number(n));
      }

      const target = this.$refs['logEntryChunk']._container
      let parent = target.parentNode

      let offset = target.offsetTop

      // Traverse to root and accumulate offset
      while (parent != _scroller) {
        offset += parent.offsetTop
        parent = parent.parentNode
      }

      _scroller.scrollTop = offset - 24 // Insure under stick header
    },
    handleLineSelect(idx: number) {
      if (this.overSize) {
        alert('Line-linking is not supported for over-sized logs')
        return
      }
      if (this.selectedLineIdx === idx) {
        this.selectedLineIdx = undefined
        this.$emit('line-deselect', idx)
        this.eventBus.emit('line-deselect', idx)
        return
      }
      this.selectedLineIdx = idx
      this.$emit('line-select', idx)
      this.eventBus.emit('line-select', idx)

    },
    toggleSettings(e) {
      this.settingsVisible = !this.settingsVisible
      e.target.blur()
    },
    toggleFollow(e) {
      this.mfollow = !this.mfollow
      e.target.blur()
    },
    closeSettings() {
      this.settingsVisible = false
    },
    toggleProgressBar() {
      this.consumeLogs = !this.consumeLogs
    },
    handleJump(e: string) {
      this.scrollToLine(this.jumpToLine || 0)
    },
    handleJumpToEnd() {
      this.scrollToLine(this.entries.length)
    },
    handleJumpToStart() {
      this.scrollToLine(1)
    },
    async populateLogs() {
      while (this.consumeLogs) {
        if (!this.resp)
          this.resp = this.viewer.getOutput(this.batchSize)
        const res = await this.resp
        this.resp = undefined

        if (!this.viewer.completed) {
          this.resp = this.viewer.getOutput(this.batchSize)
          await new Promise<void>((res, rej) => setTimeout(() => {
            res()
          }, 0))
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
  }
})
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
.fade-enter-from, .fade-leave-to * {
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
  flex: 1;
  height: 100%;
  width: 100%;
  overflow: hidden;
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
  align-self: stretch;
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
.execution-log__scroller-item-container{
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: flex-start;
  width: 100%;
  height: 100%;
}

.execution-log__warning {
  width: 100%;
  text-align: center;
}

</style>
