<template>
    <div class="execution-log__line">
        <div class="execution-log__gutter">
            <span class="gutter line-number" v-on:click="lineSelect">{{entry.node}} {{entry.time}} {{entry.id}}</span>
        </div
        ><div class="execution-log__content" v-bind:class="{'execution-log__content--html': entry.loghtml}"
            ><span v-if="entry.loghtml" v-html="entry.loghtml"
            /><span v-if="!entry.loghtml">{{entry.log}}</span>
        </div>
    </div>
</template>

<script lang="ts">
import Vue from 'vue'

export default Vue.extend({
    props: ['entry'],
    methods: {
        lineSelect: function() {
            this.$emit('line-select', this.entry.id)
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
  word-break: break-all;
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

</style>