<template>
    <li :id="item.id" class="utility-bar__item" @click="handleClick">
        <i class="utility-bar__item-icon" :class="item.class"/>
        <span v-if="item.label">{{item.label}}</span>
        <span v-if="item.count" class="utility-bar__item-counter">{{item.count}}</span>
        <Popper v-if="open" @close="close">
            <div class="card card--popover utility-bar__widget">
                <component :is="item.widget"/>
            </div>
        </Popper>
    </li>
</template>

<script lang="ts">
import Vue, {PropType} from 'vue'
import {Observer} from 'mobx-vue'

import {UtilityItem} from '../../stores/UtilityBar'

import Popper from './Popper.vue'

export default Observer(Vue.extend({
    data() { return {
        open: false
    }},
    components: {
        Popper
    },
    props: {
        item: Object as PropType<UtilityItem>
    },
    methods: {
        handleClick() {
            this.open = !this.open
        },
        close() {
            this.open = false
        },
        key() {
            return Date.now()
        }
    }
}))
</script>

<style scoped lang="scss">
.utility-bar__widget {
    margin: 0;
}
</style>