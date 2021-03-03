<template>
    <UtilActionItem v-if="item.type == 'action'" :item="item"/>
    <UtilWidgetItem v-else-if="item.type == 'widget'" :item="item"/>
</template>

<script lang="ts">
import Vue, {PropType} from 'vue'
import {Observer} from 'mobx-vue'

import {UtilityItem} from '../../stores/UtilityBar'

import UtilActionItem from './UtilityActionItem.vue'
import UtilWidgetItem from './UtilityWidgetItem.vue'

export default Observer(Vue.extend({
    components: {
        UtilActionItem,
        UtilWidgetItem
    },
    props: {
        item: Object as PropType<UtilityItem>
    }
}))
</script>
