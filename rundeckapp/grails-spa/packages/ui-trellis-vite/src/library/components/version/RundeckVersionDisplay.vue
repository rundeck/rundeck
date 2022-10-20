<template>
    <div class="rundeck-version-display">
        <i v-if="logo" :class="cssclass"/>
        <span >{{text}}</span>
    </div>
</template>

<script lang="ts">
import Vue, {PropType} from 'vue'

export default Vue.extend({
    props: {
        title: {type: String},
        number: {type: String},
        tag: {type: String},
        logo: {default: true},
        logocss: {default: 'rdicon'},
    },
    computed: {
        cssclass(): string {
            return `${this.logocss} app-logo`
        },
        text(): string {
            let {title, number, tag} = this
            let text = ''
            text += `${title ? title + ' ' : ''}`
            text += `${number}`
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
        background-size: 20px 20px;
        padding: 0;
        height: 20px;
        width: 20px;
        margin-right: 5px;
    }
}
</style>