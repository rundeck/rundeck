<template>
    <div class="execution-log__line" data-test-id="log-entry-flex-execution-log-line" v-bind:class="{'execution-log__line--selected': selected}" :title="title">
        <div v-if="displayGutter"
             class="execution-log__gutter"
             @click="onLineSelect"
             :class="{
                'execution-log__gutter--slim': (timestamps && !command)
             }"
        >
            <span class="gutter line-number">
                <span class="execution-log_gutter-entry" :pseudo-content="timestamps ? logEntry.time : ''" />
                <!-- {{timestamps ? logEntry.time : ''}} -->
                <i v-if="command" class="rdicon icon-small" v-bind:class="[logEntry.stepType]"></i>
                <span class="execution-log_gutter-entry" :pseudo-content="command ? logEntry.stepLabel : ''" />
                <!-- {{command ? logEntry.stepLabel : ''}} -->
            </span>
        </div>
        <div class="execution-log__content" v-bind:class="[`execution-log__content--level-${logEntry.level.toLowerCase()}`,
            {
                'execution-log__content--html': logEntry.logHtml
            }]"
        >
            <ui-socket section="execution-log-line" location="badges" :event-bus="eventBus" :socket-data="{
              prevEntry,
              logEntry
            }"/>
            <span v-if="displayNodeBadge" class="execution-log__node-badge"><i class="fas fa-hdd"/><span :pseudo-content="logEntry.node" /></span>
            <span v-if="logEntry.logHtml" class="execution-log__content-text" v-bind:class="{'execution-log__content-text--overflow': !lineWrap}" v-html="logEntry.logHtml" data-test-id="log-entry-content-text"/>
            <span v-if="!logEntry.logHtml" class="execution-log__content-text" v-bind:class="{'execution-log__content-text--overflow': !lineWrap}" data-test-id="log-entry-content-text">{{logEntry.log}}</span
        ></div
    ></div>
</template>

<script lang="ts">
import { defineComponent} from 'vue'
import UiSocket from '../utils/UiSocket.vue'
import {IBuilderOpts} from "./logBuilder"
import { EventBus } from '../../utilities/vueEventBus';
import type {PropType} from "vue";
import {ExecutionOutputEntry} from "../../stores/ExecutionOutput";

export default defineComponent({
    name:"EntryFlex",
    components: {
        UiSocket
    },
    props: {
        title: {
            type: String,
            required: false
        },
        eventBus: {
            type: Object as PropType<typeof EventBus>,
            required: false
        },
        config: {
          type: Object as PropType<IBuilderOpts>,
          required: true
        },
        prevEntry: {
            type: Object as PropType<ExecutionOutputEntry>,
            required: false
        },
        logEntry: {
            type: Object as PropType<ExecutionOutputEntry>,
            required: true
        },
        selected: {
            type: Boolean,
            default: false
        },
    },
    emits: ['line-select'],
    data: function() {
        return {
            cfg : this.config
        }
    },
    computed: {
        timestamps() {
            return (this.cfg.time?.visible) ? this.cfg.time.visible : false
        },
        command() {
            return (this.cfg.command?.visible) ? this.cfg.command.visible : false
        },
        gutter() {
            return (this.cfg.gutter?.visible) ? this.cfg.gutter.visible : false
        },
        nodeBadge() {
            return (this.cfg.nodeIcon) ? this.cfg.nodeIcon : false
        },
        lineWrap() {
            return (this.cfg.content?.lineWrap) ? this.cfg.content.lineWrap : false
        },
        displayNodeBadge() {
            return this.cfg.nodeIcon && (this.prevEntry == undefined || this.logEntry.node != this.prevEntry.node)
        },
        displayGutter() {
            return this.cfg.gutter?.visible && (this.cfg.time?.visible || this.cfg.command?.visible)
        }
    },
    methods: {
        onLineSelect() {
            this.$emit('line-select', this.logEntry.lineNumber)
        },
        handleSettingsChanged(newSettings: any) {
            Object.assign(this.cfg, newSettings);
        }
    },
    beforeMount() {
        this.eventBus.on("execution-log-settings-changed", this.handleSettingsChanged)
    }

})
</script>

<style lang="scss" scoped>
/** Only place critical layout and control here!
 * Theming is handled at a higher level!
 */

.execution-log__line {
  display: flex;
  width: 100%;
  font-family: monospace;
}

.execution-log__gutter {
    flex-shrink: 0;
    flex-grow: 0;
    flex-basis: 200px;
    user-select: none;
    display: flex;
    align-items: center;
    min-width: 0;
    cursor: pointer;

    & i {
        margin-left: 0.5em;
        margin-right: 0.2em;
    }
}

.execution-log__gutter--slim {
    flex-basis: 100px;
}

.execution-log__gutter span {
    text-overflow: ellipsis;
    white-space: nowrap;
    overflow: hidden;
}

.execution-log__content {
    flex-grow: 1;
    white-space: pre-wrap;
    min-width: 0;
}

.execution-log__content-text {
    word-break: break-word;
}

.execution-log__content-text--overflow {
    overflow: hidden;
    white-space: pre;
    text-overflow: ellipsis;
    display: block;
    word-break: normal;
}

.execution-log__content--html {
    flex-grow: 1;
    white-space: pre-wrap;
}

.execution-log__line-number {
    cursor: pointer;
}

.execution-log__node-badge {
    float: right;
    user-select: none;
    & i {
        margin-left: 0.2em;
        margin-right: 0.2em;
    }
}

[pseudo-content]::before {
    content: attr(pseudo-content);
}

</style>
