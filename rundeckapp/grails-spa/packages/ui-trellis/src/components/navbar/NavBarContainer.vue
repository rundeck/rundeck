<template>
    <li :id="item.id" class="navbar__item-container" :class="{'navbar__item--active': item.active}" sytle="display: flex;">
        <NavBarDrawer>
            <a :href="item.link">
                <i style="" :class="item.class"/>
                <div>{{label}}</div>
            </a>
            <template slot="content">
                <ul 
                    :class="{
                        'navbar__container--icons': item.style == 'icon',
                        'navbar__container--list': item.style == 'list'
                    }">
                <NavBarItem v-for="entry in navBar.containerItems(item.id)" :item="entry" :key="entry.id" :itemStyle="item.style" />
                </ul>
            </template>
        </NavBarDrawer>
    </li>
</template>

<script lang="ts">
import Vue from 'vue'

import {NavItem, NavBar, NavContainer} from '../../stores/NavBar'

import NavBarItem from './NavBarItem.vue'
import {Component, Inject, Prop} from 'vue-property-decorator'
import { Observer } from 'mobx-vue'
import { RootStore } from '../../stores/RootStore'

import NavBarDrawer from './NavBarDrawer.vue'

@Observer
@Component({components: {NavBarItem, NavBarDrawer}})
export default class NavBarContainer extends Vue {
    @Inject()
    rootStore!: RootStore

    navBar!: NavBar

    @Prop()
    item!: NavContainer

    created() {
        this.navBar = this.rootStore.navBar
    }

    body() { 
        return document.body
    }

    get label(): string {
        return this.item!.label!.toUpperCase()
    }
}
</script>

<style lang="scss" scoped>

a {
    width: 100%;
}

.navbar__item-container {
    position: relative;
    list-style-type: none;
    display: flex;
    width: 100%;
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

    i {
        height: 24px !important;
        width: 24px !important;
        font-size: 24px;
        margin-bottom: 5px;
        background-size: 24px !important;
    }

}

.navbar__container--icons {
    display: flex;
    flex-direction: row;
    width: 100%;
    flex-wrap: wrap;
    margin: 0;
    padding: 10px;
    align-items: start;
    align-content: baseline;

    i {
        height: 24px !important;
        width: 24px !important;
        font-size: 24px;
        margin-bottom: 5px;
        background-size: 24px !important;
    }

    li {
        width: 40px;
        margin: 10px;
    }
}

.navbar__container--list {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    padding-left: 20px;

    // i {
    //     height: 24px !important;
    //     width: 24px !important;
    //     font-size: 24px;
    //     margin-bottom: 5px;
    //     background-size: 24px !important;
    // }

    // li {
    //     width: 40px;
    //     margin: 10px;
    // }
}

</style>

<style lang="scss">
.v-popover, .v-popover .trigger {
    width: 100%;
}

.navbar-popover, .navbar-popover * {
    outline: none !important;
    :focus {
        outline: none !important;
    }
}

.navbar-popover-inner {
    border-radius: 5px 10px 10px 5px;
    border-style: solid;
    border-color: #414141;
    border-width: 1px;
    background-color: #303030;
    height: 100vh;
    width: 200px;
    padding: 10px;
}
</style>