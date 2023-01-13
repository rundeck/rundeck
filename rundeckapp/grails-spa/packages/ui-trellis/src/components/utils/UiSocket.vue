<template>
    <span v-if="items">
        <slot v-if="items.length<1"></slot>
        <template v-for="i in items">
            <template v-if="i.text">{{ i.text }}</template>
            <span v-else-if="i.html" v-html="i.html"></span>
            <component :is="i.widget" v-else-if="i.widget"/>
        </template>
    </span>
</template>
<script lang="ts">

import Vue from 'vue'
import {Component, Prop, Watch} from 'vue-property-decorator'

import {
    getRundeckContext,
} from '../../rundeckService'
import {UIItem, UIWatcher} from '../../stores/UIStore'
// import {UIItem, UIWatcher} from '@rundeck/ui-trellis/lib/stores/UIStore'

const rootStore = getRundeckContext().rootStore

@Component
export default class UiSocket extends Vue {
    @Prop({required: true}) readonly section!: string
    @Prop({required: true}) readonly location!: string
    @Prop({required: false}) readonly eventBus!: Vue
    @Prop({required: false}) readonly data!: any

    items: UIItem[] = []

    loadItems() {
        this.items = rootStore.ui.itemsForLocation(this.section, this.location).filter((a) => a.visible)
    }

    destroyed() {
        if(this.uiwatcher){
            rootStore.ui.removeWatcher(this.uiwatcher)
        }
    }

    uiwatcher: UIWatcher|null=null


    mounted() {
        this.loadItems()
        this.uiwatcher = {
            section: this.section,
            location: this.location,
            callback: (items: UIItem[])=>{
                this.items=items
            }
        }
        rootStore.ui.addWatcher(this.uiwatcher)
    }
}
</script>
