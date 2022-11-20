<template>
    <span>
        <slot v-if="items.length<1"></slot>
        <span v-for="i in items">
            <component :is="i.widget"/>
        </span>

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
export default class UIComponent extends Vue {
    @Prop({required: true}) readonly section!: string
    @Prop({required: true}) readonly location!: string
    @Prop({required: false}) readonly eventBus!: Vue

    items: UIItem[] = []

    mounted() {
        this.items = rootStore.ui.itemsForLocation(this.section, this.location)
        this.eventBus.$emit('ui-component-mounted', {section: this.section, location: this.location})
    }
}
</script>