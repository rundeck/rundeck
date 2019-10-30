<template>
    <div class="execution-log__line">
        <div class="execution-log__gutter">
            <span class="gutter line-number" v-on:click="lineSelect">{{entry.time}} {{stepLabel()}}</span>
        </div
        ><div class="execution-log__content" v-bind:class="{'execution-log__content--html': entry.loghtml}"
            ><span v-if="nodeBadge" class="execution-log__node-badge"><i class="fas fa-hdd"/>{{entry.node}}</span
            ><span v-if="entry.loghtml" v-html="entry.loghtml"
            /><span v-if="!entry.loghtml">{{entry.log}}</span
        ></div
    ></div>
</template>

<script lang="ts">
import Vue, {PropType} from 'vue'

export default Vue.extend({
    props: {
        entry: {
            type: Object as any,
            default: {} as any
        },
        nodeBadge: {
            type: Boolean,
            default: false
        }
    },
    methods: {
        lineSelect: function() {
            this.$emit('line-select', this.entry.id)
        },
        stepLabel: function() {
            const lastStep = this.entry.renderedStep[this.entry.renderedStep.length -1]
            if (lastStep)
                return `${lastStep.stepNumber.trim()}${lastStep.label}`
            else
                return this.entry.stepctx
        }
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
  word-break: break-word;
}

.execution-log__gutter {
    flex-shrink: 0;
    flex-grow: 0;
    flex-basis: 200px;
    user-select: none;
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