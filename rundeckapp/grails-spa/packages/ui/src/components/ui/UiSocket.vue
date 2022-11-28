<template>
    <span>
        <slot v-if="items.length<1"></slot>
        <template v-for="i in items">
            <template v-if="i.text">{{i.text}}</template>
            <template v-else-if="i.html" v-html="i.html"></template>
            <component :is="i.widget" v-else-if="i.widget"/>
        </template>
    </span>
</template>
<script lang="ts">

import Vue from 'vue'
import VueI18n from 'vue-i18n'
import {Component, Prop, Watch} from 'vue-property-decorator'

import {
    getRundeckContext,
    RundeckContext
} from '@rundeck/ui-trellis'
import {UIItem} from '@rundeck/ui-trellis/lib/stores/UIStore'

const rootStore = getRundeckContext().rootStore

@Component
export default class UiSocket extends Vue {
    @Prop({required: true}) readonly section!: string
    @Prop({required: true}) readonly location!: string
    @Prop({required: false}) readonly eventBus!: Vue

    items: UIItem[] = []

    mounted() {
        this.items = rootStore.ui.itemsForLocation(this.section, this.location)
        if(this.eventBus) {
            this.eventBus.$emit('ui-socket-mounted', {section: this.section, location: this.location})
        }
    }
}
</script>