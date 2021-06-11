<template>
    <FilterList :items="webhooks.webhooks">
        <template slot="item" scope="{item}"><img style="height: 20px; width: 20px;border-radius: 4px;" :src="item.eventPlugin.iconUrl"/> {{item.name}}</template>
    </FilterList>
</template>


<script lang="ts">
import Vue from 'vue'

import {Component, Inject} from 'vue-property-decorator'
import {Observer} from 'mobx-vue'

import {RootStore} from '../../../stores/RootStore'
import { PluginStore } from '../../../stores/Plugins'
import { WebhookStore } from '../../../stores/Webhooks'

import FilterList from '../../filter-list/FilterList.vue'


@Observer
@Component({components: {FilterList}})
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