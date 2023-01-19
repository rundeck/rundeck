<template>
    <div class="execution-log__line" v-bind:class="{'execution-log__line--selected': selected}">
        <div v-if="displayGutter" class="execution-log__gutter" v-on:click="lineSelect"
            v-bind:class="{
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
              prevEntry: this.prevEntry,
              logEntry: this.logEntry
            }"/>
            <span v-if="displayNodeBadge" class="execution-log__node-badge"><i class="fas fa-hdd"/><span :pseudo-content="logEntry.node" /></span>
            <span v-if="logEntry.logHtml" class="execution-log__content-text" v-bind:class="{'execution-log__content-text--overflow': !lineWrap}" v-html="logEntry.logHtml"/>
            <span v-if="!logEntry.logHtml" class="execution-log__content-text" v-bind:class="{'execution-log__content-text--overflow': !lineWrap}">{{logEntry.log}}</span
        ></div
    ></div>
</template>

<script lang="ts">
import Vue, {PropType} from 'vue'
import {Component, Prop} from 'vue-property-decorator'
import UiSocket from '../utils/UiSocket.vue'

@Component({components: {UiSocket}})
export default class Flex extends Vue {
    @Prop({required: false})
    readonly eventBus!: Vue
  
    @Prop({default: false})
    selected!: boolean

    @Prop({default: false})
    timestamps!: boolean

    @Prop({default: true})
    command!: boolean

    @Prop({default: true})
    gutter!: boolean

    @Prop({default: true})
    nodeBadge!: boolean

    @Prop({default: true})
    lineWrap!: boolean

    @Prop({required: false, default: undefined})
    prevEntry!: any

    @Prop({required: true})
    logEntry!: any

    get displayNodeBadge() {
        return this.nodeBadge && (this.prevEntry == undefined || this.logEntry.node != this.prevEntry.node)
    }

    get displayGutter() {
        return this.gutter && (this.timestamps || this.command)
    }

    lineSelect() {
        this.$emit('line-select', this.logEntry.lineNumber)
    }

}
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
