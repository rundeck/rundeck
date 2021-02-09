<template>
    <li :class="{active: item.active}" sytle="display: flex;">
        <NavBarDrawer>
            <a :href="item.link">
                <i style="" :class="item.class"/>
                <div>{{label}}</div>
            </a>
            <template slot="content">
                <ul style="display: flex; flex-direction: row; width: 100%;">
                <NavBarItem v-for="entry in navBar.overflow" :item="entry" :key="entry.id" />
                </ul>
            </template>
        </NavBarDrawer>
    </li>
</template>

<script lang="ts">
import Vue, {PropType} from 'vue'

import {VPopover} from 'v-tooltip'

import {NavItem, NavBar} from '../../stores/NavBar'

import NavBarItem from './NavBarItem.vue'
import {Component, Inject, Prop} from 'vue-property-decorator'
import { Observer } from 'mobx-vue'
import { RootStore } from 'src/stores/RootStore'

import NavBarDrawer from './NavBarDrawer.vue'

@Observer
@Component({components: {NavBarItem, NavBarDrawer, VPopover}})
export default class NavBarContainer extends Vue {
    @Inject()
    rootStore!: RootStore

    navBar!: NavBar

    @Prop()
    item!: NavItem

    created() {
        this.navBar = this.rootStore.navBar
    }

    body() { 
        return document.body
    }

    get label(): string {
        return this.item!.label!.toUpperCase()
    }

    deactivated() {
        console.log('Deactivated')
    }

    destroyed() {
        console.log('Unmounted')
    }
}
</script>

<style lang="scss" scoped>

.active * {
    color: white;
}

a {
    width: 100%;
}

li {
    position: relative;
    list-style-type: none;
    display: flex;
    width: 100%;
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