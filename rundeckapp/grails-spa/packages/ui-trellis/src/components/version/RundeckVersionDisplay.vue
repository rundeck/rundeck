<template>
    <div class="rundeck-version-display">
        <i class="rdicon app-logo"/>
        <span >{{text}}</span>
    </div>
</template>

<script lang="ts">
import Vue from 'vue'

export default Vue.extend({
    props: {
        edition: {type: String},
        number: {type: String},
        tag: {type: String}
    },
    computed: {
        text(): string {
            let {edition, number, tag} = this
            let text = `Rundeck ${edition} ${number}`
            return text
        }
    }
})
</script>

<style scoped lang="scss">
.rundeck-version-display {
    font-size: 24px;
    font-weight: 600;
    display: flex;
    align-content: center;
    align-items: center;

    >i {
        background-size: 20px;
        padding: 0;
        height: 20px;
        width: 20px;
    }

    >span {
        margin-left: 5px;
    }
}
</style>