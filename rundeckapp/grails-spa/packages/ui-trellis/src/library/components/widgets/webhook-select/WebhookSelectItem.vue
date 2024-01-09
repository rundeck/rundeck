<template>
    <div class="webhook-select__item" style="height: 40px;display: flex; align-items: center;">
        <PluginInfo :detail="webhook.eventPlugin" :showTitle="false" :showDescription="false"/>
        <span style="margin-left: 10px; font-weight: 600;">{{webhook.name}}</span>
    </div>
</template>

<script lang="ts">
import { Webhook } from '../../../stores/Webhooks'
import PluginInfo from '../../plugins/PluginInfo.vue'
import {defineComponent} from "vue";
import type {PropType} from "vue";

export default defineComponent({
    name:"WebhookSelectItem",
    components: {
        PluginInfo
    },
    props: {
        webhook: {
            type: Object as PropType<Webhook>,
            required: true
        }
    }
})

</script>

<style scoped lang="scss">
:deep(.plugin-icon) {
    border-radius: 4px;
    height: 20px;
    width: 20px;
}
</style>