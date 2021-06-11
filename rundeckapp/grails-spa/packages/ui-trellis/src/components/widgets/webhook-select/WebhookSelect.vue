<template>
    <FilterList :items="webhooks.webhooks" searchText="Filter Webhooks" :itemSize="40">
        <template slot="item" scope="{item}">
            <div style="height: 40px;display: flex; align-items: center;">
                <PluginInfo :detail="item.eventPlugin" :showTitle="false" :showDescription="false"/>
                <span style="margin-left: 10px; font-weight: 600;">{{item.name}}</span>
            </div>
        </template>
    </FilterList>
</template>


<script lang="ts">
import Vue from 'vue'

import {Component, Inject} from 'vue-property-decorator'
import {Observer} from 'mobx-vue'

import {RootStore} from '../../../stores/RootStore'
import { PluginStore } from '../../../stores/Plugins'
import { WebhookStore } from '../../../stores/Webhooks'

import PluginInfo from '../../plugins/PluginInfo.vue'

import FilterList from '../../filter-list/FilterList.vue'


@Observer
@Component({components: {FilterList, PluginInfo}})
export default class PluginSelect extends Vue {
    @Inject()
    private readonly rootStore!: RootStore

    plugins!: PluginStore

    webhooks!: WebhookStore

    created() {
        this.plugins = this.rootStore.plugins
        this.webhooks = this.rootStore.webhooks

        this.webhooks.load('Test')
    }
}
</script>

<style scoped lang="scss">
::v-deep .plugin-icon {
    height: 20px !important;
    width: 20px !important;
}
</style>