<template>
    <li :id="item.id" class="navbar__item" :class="{'navbar__item--active': item.active}" sytle="display: flex;">
        <a :href="item.link">
            <i style="" :class="item.class"/>
            <div>{{label}}</div>
        </a>
    </li>
</template>

<script lang="ts">
import Vue, {PropType} from 'vue'

import {NavItem} from '../../stores/NavBar'

export default Vue.extend({
    props: {
        item: Object as PropType<NavItem>
    },
    computed: {
        label(): string {
            return this.item!.label!.toUpperCase()
        }
    }
})
</script>

<style lang="scss" scoped>

.navbar__item {
    list-style-type: none;
    // width: 100%;
    display: flex;
    flex-direction: column;
    align-content: center;
    align-items: center;
    margin-top: 20px;
    font-size: 8px;
    font-weight: 800;
    text-align: center;

    * {
        color: #BCBCBC
    }

    :hover * {
        color: white;
    }

    &.navbar__item--active * {
        color: white;
    }
}

i {
    height: 24px !important;
    width: 24px !important;
    font-size: 24px;
    margin-bottom: 5px;
    background-size: 24px !important;
}
</style>