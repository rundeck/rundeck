<template>
    <li :id="item.id" class="utility-bar__item" @click="item.action">
        <i class="utility-bar__item-icon" :class="item.class"/>
        <span>{{item.label}}</span>
        <span v-if="item.count" class="utility-bar__item-counter">{{item.count}}</span>
    </li>
</template>

<script lang="ts">
import Vue, {PropType} from 'vue'
import {Observer} from 'mobx-vue'

import {UtilityItem} from '../../stores/UtilityBar'

export default Observer(Vue.extend({
    props: {
        item: Object as PropType<UtilityItem>
    }
}))
</script>
