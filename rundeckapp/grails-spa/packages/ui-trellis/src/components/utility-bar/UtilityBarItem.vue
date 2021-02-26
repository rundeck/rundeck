<template>
    <li class="utility-bar__item" @click="item.action">
        <i :class="item.class"/>
        <span>{{item.label}}</span>
    </li>
</template>

<script lang="ts">
import Vue, {PropType} from 'vue'

import {UtilityItem} from '../../stores/UtilityBar'

export default Vue.extend({
    props: {
        item: Object as PropType<UtilityItem>
    }
})
</script>
