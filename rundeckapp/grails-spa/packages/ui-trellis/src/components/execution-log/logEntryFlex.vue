<template>
    <div class="execution-log__line" v-bind:class="{'execution-log__line--selected': selected}">
        <div v-if="gutter" class="execution-log__gutter" v-on:click="lineSelect">
            <span class="gutter line-number">
                {{timestamp ? entry.time : ''}}
                <i class="rdicon icon-small" v-bind:class="[entry.stepType]"></i>
                {{entry.stepLabel}}
            </span>
        </div
        ><div class="execution-log__content" v-bind:class="[`execution-log__content--level-${entry.level.toLowerCase()}`,
            {
                'execution-log__content--html': entry.logHtml
            }]"
            ><span v-if="entry.nodeBadge" class="execution-log__node-badge"><i class="fas fa-hdd"/> {{entry.node}}</span
            ><span v-if="entry.logHtml" v-html="entry.logHtml"
            /><span v-if="!entry.logHtml">{{entry.log}}</span
        ></div
    ></div>
</template>

<script lang="ts">
import Vue, {PropType} from 'vue'
import { Component, Prop } from 'vue-property-decorator'

@Component
export default class Flex extends Vue {
    @Prop({default: false})
    selected!: boolean

    @Prop({default: false})
    timestamp!: boolean

    @Prop({default: true})
    gutter!: boolean

    lineSelect() {
        this.$emit('line-select', (<any>this).entry.lineNumber)
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
}

.execution-log__content {
    flex-grow: 1;
    white-space: pre-wrap;
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