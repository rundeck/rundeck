<template>
    <li 
        :id="item.id"
        class="navbar__item"
        :class="{
            'navbar__item--icon': itemStyle == 'icon',
            'navbar__item--list': itemStyle == 'list',
            'navbar__item--active': item.active
        }">
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
        itemStyle: {
            default: 'icon',
            type: String as PropType<'icon' | 'list'>
        },
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
    font-weight: 800;
    text-align: center;

    * {
        // color: #BCBCBC;
        color: var(--sidebar-item-color);
    }

    :hover * {
        color: var(--sidebar-item-state-color);
    }

    &.navbar__item--active * {
        color: var(--sidebar-item-state-color);
    }
}

.navbar__item--icon {
    display: flex;
    flex-direction: column;
    align-content: center;
    align-items: center;
    margin-top: 20px;
    font-size: 8px;

    i {
        height: 24px !important;
        width: 24px !important;
        font-size: 24px;
        margin-bottom: 5px;
        background-size: 24px !important;
    }
}

.navbar__item--list {
    display: flex;
    flex-direction: column;
    align-content: center;
    align-items: center;
    margin-top: 20px;
    font-size: 12px;

    * {
        display: inline-block;
    }

    i {
        margin-right: 10px;
    }
}


</style>