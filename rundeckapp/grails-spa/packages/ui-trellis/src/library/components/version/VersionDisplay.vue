<template>
    <popover trigger="hover" :enable="showTooltip">
        <span>
            <span v-if="showAppName" class="rundeck-version-info">
                {{appName}}
            </span>
            <span v-if="showVersion" class="rundeck-version-info">
                {{versionSemantic}}
            </span>

            <version-date-display v-if="isGa && showDate" :title="date" :date="date"/>

            <span v-if="!isGa && (showDate||showRelativeDate||showTag)">
                <span v-if="showTag">
                    <span v-if="versionTag==='SNAPSHOT'">🛠</span>
                    <span v-else class="rundeck-version-tag" :style="versionColorStyle">{{versionTag}}</span>
                </span>
                <span v-if="showRelativeDate"
                      class="rundeck-version-relative-date">{{date | moment("from", "now")}}</span>
                <version-date-display v-if="showDate" :title="date" :date="date"/>
            </span>

            <version-icon-name-display :icon="versionIcon" :text="versionText" :color="versionColor" v-if="showName"/>
        </span>
        <template slot="popover">
            {{version}}
            <version-date-display :date="date"/>
            <version-icon-name-display :icon="versionIcon" :text="versionText" :color="versionColor"/>
        </template>
    </popover>
</template>

<script lang="ts">
import Vue from 'vue'
import {RundeckVersion} from '../../utilities/RundeckVersion'
import VersionIconNameDisplay from './VersionIconNameDisplay.vue'
import VersionDateDisplay from './VersionDateDisplay.vue'

export default Vue.extend({
  components: {
    VersionIconNameDisplay,
    VersionDateDisplay
  },
  props: {
    version: {
      type: String, required: true
    },
    date: {
      type: String, required: false
    },
    appName: {
      type: String, required: false
    },
    showAppName: {
      type: Boolean, required: false, default: true
    },
    showName: {
      type: Boolean, required: false, default: true
    },
    showVersion: {
      type: Boolean, required: false, default: true
    },
    showDate: {
      type: Boolean, required: false, default: true
    },
    showRelativeDate: {
      type: Boolean, required: false, default: false
    },
    showTag: {
      type: Boolean, required: false, default: true
    },
    showTooltip: {
      type: Boolean, required: false, default: false
    }
  },
  data() {
    return {
      rdversion: null as (RundeckVersion | null)
    }
  },
  computed: {
    versionTag(): string {
      return this.rdversion !== null ? this.rdversion.data().tag : ''
    },
    isGa(): boolean {
      return !(this.versionTag && this.versionTag !== 'GA')
    },
    versionSemantic(): string {
      return this.rdversion !== null ? this.rdversion.versionSemantic() : ''
    },
    versionName(): string {
      return this.rdversion !== null ? this.rdversion.versionName() : ''
    },
    versionIcon(): string {
      return this.rdversion !== null ? this.rdversion.versionIcon() : ''
    },
    versionIconCss(): string {
      return this.rdversion !== null ? 'glyphicon-' + this.rdversion.versionIcon() : ''
    },
    versionColor(): string {
      return this.rdversion !== null ? this.rdversion.versionColor() : ''
    },
    versionColorStyle(): any {
      return {
        color: this.versionColor
      }
    },
    versionText(): string {
      return [this.versionName, this.versionColor, this.versionIcon].join(' ').toLowerCase()
    },
    displayTitle(): string {
      return `${this.appName} ${this.version} (${this.versionText}) ${this.date}`
    },
    badgeStyle(): any {
      let rdversion = this.rdversion
      let color = this.versionColor
      return rdversion !== null ? {
        background: rdversion.stripeBg(color, 15, '#5c5c5c', 20),
        color: 'white'
      } : {}
    }
  },
  watch:{
    version(){
        this.loadversion()
    },
  },
  methods:{
    loadversion(){
        this.rdversion = new RundeckVersion({versionString: this.version, versionDate: this.date, appId: this.appName})
    }
  },
  mounted() {
    this.loadversion()
  }
})
</script>
<style>
    .rundeck-version-info {
        color: #8a8a8a;
    }

    .rundeck-version-date {
        color: lightgray;
        font-style: italic;
    }

    .rundeck-version-tag {
        text-transform: uppercase;
    }

    .rundeck-version-relative-date {
        text-decoration: underline;
    }
</style>
