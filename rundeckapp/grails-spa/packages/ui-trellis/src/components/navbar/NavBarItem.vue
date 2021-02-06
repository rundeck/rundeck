<template>
    <li v-if="item.type == 'link'" :class="{active: item.active}" sytle="display: flex;">
        <a :href="item.link">
            <i style="" :class="item.class"/>
            <div>{{label}}</div>
        </a>
    </li>
    <NavBarContainer v-else :item="item"/>
</template>

<script lang="ts">
import Vue, {PropType} from 'vue'

import {NavItem} from '../../stores/NavBar'

import NavBarContainer from './NavBarContainer.vue'

export default Vue.extend({
    components: {
        NavBarContainer
    },
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

.active * {
    color: white;
}

li {
    list-style-type: none;
    display: flex;
    flex-direction: column;
    align-content: center;
    align-items: center;
    margin-top: 20px;
    margin-bottom: 20px;
    font-size: 8px;
    font-weight: 800;
    text-align: center;

    * {
        color: #BCBCBC
    }

    :hover * {
        color: white;
    }

    @media(max-width: 768px) {
        margin: 0px 5px 0px;
        width: 65px;
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