<template>
    <div>
        <slot/>
    </div>
</template>

<script lang="ts">
import {defineComponent} from 'vue'

export default defineComponent({
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