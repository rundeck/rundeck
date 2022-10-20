<template>
    <div class="help-icon">
        <i :id="`help-${this._uid}`" class="fas fa-sm fa-question-circle text-muted" style="cursor:pointer;margin-left:5px"/>
        <popover :title="title" :target="`#help-${this._uid}`" custom-class="popover-wide" viewport="#section-main">
        <template slot="popover">
            <slot></slot>
        </template>
        </popover>
    </div>
</template>

<script lang="ts">
import Vue from 'vue'

export default Vue.extend({
    name: 'HelpIcon',
    props: {
        title: String
    }
})
</script>

<style>
.popover-content {
    max-height: 50vh;
    overflow-y: auto;
}
</style>

<style scoped>
.help-icon {
    display: inline;
}
</style>
