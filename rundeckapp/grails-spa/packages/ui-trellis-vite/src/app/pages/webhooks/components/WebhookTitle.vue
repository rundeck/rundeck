<template>
    <span class="webhook-title">
        <PluginInfo v-if="webhook.eventPlugin" style="margin-right: 10px" :detail="webhook.eventPlugin" :showTitle="false" :showDescription="false"/><h3>{{webhook.name}}</h3>
    </span>
</template>


<script lang="ts">
import Vue from 'vue'

import PluginInfo from '@/library/components/plugins/PluginInfo.vue'

import {Webhook} from '@/library/stores/Webhooks'

export default Vue.extend({
    components: {
        PluginInfo
    },
    props: {
        webhook: {type: Object}
    }
})
</script>

<style scoped lang="scss">
::v-deep .plugin-icon {
    border-radius: 4px;
    height: 35px !important;
    width: 35px !important;
}

.webhook-title {
    display: flex;
    align-items: center;
    align-content: center;

    h3 {
      color: var(--font-color);
      font-weight: 700;
      margin: 0;
    }
}
</style>