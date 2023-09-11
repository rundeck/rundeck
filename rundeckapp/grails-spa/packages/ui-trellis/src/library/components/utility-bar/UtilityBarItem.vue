<template>
    <UtilActionItem v-if="item.type === 'action'" :item="(item as UtilityActionItem)"/>
    <UtilWidgetItem v-else-if="item.type === 'widget'" :item="(item as UtilityWidgetItem)"/>
</template>

<script lang="ts">
import {defineComponent, PropType} from 'vue'

import {UtilityActionItem, UtilityItem, UtilityWidgetItem} from '../../stores/UtilityBar'

import UtilActionItem from './UtilityActionItem.vue'
import UtilWidgetItem from './UtilityWidgetItem.vue'

export default defineComponent({
    name: 'UtilityBarItem',
    components: {
        UtilActionItem,
        UtilWidgetItem
    },
    props: {
        item: {
          type: Object as PropType<UtilityItem>,
          required: true,
        }
    }
})
</script>
