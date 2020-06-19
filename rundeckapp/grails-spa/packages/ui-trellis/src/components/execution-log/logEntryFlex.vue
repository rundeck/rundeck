<template>
    <div class="execution-log__line" v-bind:class="{'execution-log__line--selected': selected}">
        <div v-if="gutter" class="execution-log__gutter" v-on:click="lineSelect">
            <span class="gutter line-number">
                {{timestamps ? $options.entry.time : ''}}
                <i class="rdicon icon-small" v-bind:class="[$options.entry.stepType]"></i>
                {{$options.entry.stepLabel}}
            </span>
        </div
        ><div class="execution-log__content" v-bind:class="[`execution-log__content--level-${$options.entry.level.toLowerCase()}`,
            {
                'execution-log__content--html': $options.entry.logHtml
            }]"
            ><span v-if="displayNodeBadge" class="execution-log__node-badge"><i class="fas fa-hdd"/> {{$options.entry.node}}</span
            ><span v-if="$options.entry.logHtml" v-bind:class="{'execution-log__content-text--overflow': !lineWrap}" v-html="$options.entry.logHtml"
            /><span v-if="!$options.entry.logHtml" v-bind:class="{'execution-log__content-text--overflow': !lineWrap}">{{$options.entry.log}}</span
        ></div
    ></div>
</template>

<script lang="ts">
import Vue, {PropType} from 'vue'
import { Component, Prop } from 'vue-property-decorator'
import { ComponentOptions } from 'vue/types/umd'

@Component
export default class Flex extends Vue {
    @Prop({default: false})
    selected!: boolean

    @Prop({default: false})
    timestamps!: boolean

    @Prop({default: true})
    gutter!: boolean

    @Prop({default: true})
    nodeBadge!: boolean

    @Prop({default: true})
    lineWrap!: boolean

    get displayNodeBadge() {
        return this.$options.entry.nodeBadge && this.nodeBadge
    }

    $options!: ComponentOptions<Vue> & {
        entry: any
    }

    lineSelect() {
        this.$emit('line-select', this.$options.entry.lineNumber)
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
  word-break: break-word;
}

.execution-log__gutter {
    flex-shrink: 0;
    flex-grow: 0;
    flex-basis: 200px;
    user-select: none;
    display: flex;
    align-items: center;
    min-width: 0;
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

.execution-log__content-text--overflow {
    overflow: hidden;
    white-space: pre;
    text-overflow: ellipsis;
    display: block;
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
}

</style>