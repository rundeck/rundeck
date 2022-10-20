<template>
    <div>
        <slot/>
    </div>
</template>

<script lang="ts">
import Vue from 'vue'

export default Vue.extend({
    name: 'rd-tab-pane',
    created() {
        console.log('content created')
    },
    mounted() {
        console.log('content mounted')
    },
    activated() {
        console.log('content activated')
    }
})
</script>