<template>
    <FilterList v-on="$listeners" :items="webhooks.webhooksForProject(project)" id-field="uuid" :selected="selected" searchText="Filter Webhooks" :itemSize="40">
        <template v-slot:item="{item}"  >
            <WebhookItem :webhook="item"/>
        </template>
    </FilterList>
</template>


<script lang="ts">
import Vue from 'vue'

import {Component, Inject, Prop} from 'vue-property-decorator'
import {Observer} from 'mobx-vue'

import {RootStore} from '../../../stores/RootStore'
import { PluginStore } from '../../../stores/Plugins'
import { WebhookStore } from '../../../stores/Webhooks'

import PluginInfo from '../../plugins/PluginInfo.vue'

import FilterList from '../../filter-list/FilterList.vue'

import WebhookItem from './WebhookSelectItem.vue'

@Observer
@Component({components: {FilterList, PluginInfo, WebhookItem}})
export default class PluginSelect extends Vue {
    @Inject()
    private readonly rootStore!: RootStore

    @Prop()
    project!: string

    @Prop({default: ''})
    selected!: string

    plugins!: PluginStore

    webhooks!: WebhookStore

    created() {
        this.plugins = this.rootStore.plugins
        this.webhooks = this.rootStore.webhooks

        this.webhooks.load(this.project)
    }

    itemUpdated() {
        console.log('Updated webhook select')
    }
}
</script>

<style scoped lang="scss">
::v-deep .plugin-icon {
    height: 20px !important;
    width: 20px !important;
}

::v-deep .scroller__item {
    border-radius: 5px;
    padding-left: 10px;

    &--selected, &:hover {
        background-color: var(--background-color-accent);
    }

    &:before {
        content: none !important;
    }
}
</style>