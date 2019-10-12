<template>
    <div class="log-line">
        <div class="log-gutter">
            <span class="gutter">{{entry.node}}</span>
        </div>
        <div class="log-content">
            <span v-if="entry.loghtml" v-html="entry.loghtml"/>
            <span v-if="!entry.loghtml">{{entry.log}}</span>
        </div>
    </div>
</template>

<script lang="ts">
import Vue from 'vue'

export default Vue.extend({
    props: ['entry']
})
</script>

<style lang="scss" scoped>
.log-line {
  display: flex;
  width: 100%;
  font-family: monospace;
  word-break: break-all;
}

.log-gutter {
    flex-shrink: 0;
    flex-grow: 0;
    flex-basis: 200px;
    user-select: none;
}

.log-content {
    flex-grow: 1;
}

</style>