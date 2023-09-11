<template>
    <li :id="item.id" class="utility-bar__item" @click="item.action">
        <i class="utility-bar__item-icon" :class="item.class"></i>
        <span>{{item.label}}</span>
        <span v-if="item.count" class="utility-bar__item-counter">{{item.count}}</span>
    </li>
</template>

<script lang="ts">
import  {defineComponent, PropType} from 'vue'

import {UtilityActionItem} from '../../stores/UtilityBar'

export default defineComponent({
    name: 'UtilityActionItem',
    props: {
        item: {
            type: Object as PropType<UtilityActionItem>,
            required: true,
        }
    }
})
</script>
