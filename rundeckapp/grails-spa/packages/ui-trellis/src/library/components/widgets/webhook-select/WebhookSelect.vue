<template>
    <FilterList v-bind="$attrs" :items="webhooksForProject" id-field="uuid" :selected="selected" searchText="Filter Webhooks" :itemSize="40">
        <template v-slot:item="{item}"  >
            <WebhookSelectItem :webhook="item"/>
        </template>
    </FilterList>
</template>


<script lang="ts">
import {defineComponent, onBeforeMount, ref} from 'vue'

import { PluginStore } from '../../../stores/Plugins'
import { WebhookStore } from '../../../stores/Webhooks'

import FilterList from '../../filter-list/FilterList.vue'

import WebhookSelectItem from './WebhookSelectItem.vue'
import {RundeckContext} from "../../../interfaces/rundeckWindow";
import {getRundeckContext} from "../../../rundeckService";

export default defineComponent({
    name: "WebhookSelect",
    inject: ['rootStore'],
    components: {
        FilterList,
        WebhookSelectItem
    },
    props: {
        project: {
            type: String,
            required: true
        },
        selected: {
            type: String,
            default: ''
        }
    },
    computed: {
      webhooksForProject() {
          return this.webhookStore.webhooks.filter( wh => wh.project == this.project) || []
      }
    },
    data() {
        return {
            webhookStore: window._rundeck.rootStore.webhooks
        }
    },
    beforeMount() {
        this.webhookStore.load(this.project)
    }
})
</script>

<style scoped lang="scss">
:deep(.plugin-icon) {
    height: 20px !important;
    width: 20px !important;
}

:deep(.scroller__item) {
    border-radius: 5px;
    padding-left: 10px;

    &:hover {
        background-color: var(--background-color-accent);
    }

    &:before {
        content: none !important;
    }
}

:deep(.scroller__item--selected) {
  background-color: var(--background-color-accent);
}
</style>