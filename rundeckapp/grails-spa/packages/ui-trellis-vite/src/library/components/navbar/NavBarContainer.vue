<template>
    <li :id="item.id" class="navbar__item-container" :class="{'navbar__item--active': item.active}" sytle="display: flex;">
        <div class="navbar__item-spacer"></div>
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
    padding: 8px 0;
    border-top: 1px solid transparent;
    border-bottom: 1px solid transparent;
    border-left: 1px solid transparent;
    * {
        color: var(--sidebar-item-color);
    }
    :hover{
        cursor: pointer;
    }
    :hover * {
        color: var(--sidebar-item-state-color);
    }
    &.navbar__item--active * {
        color: var(--sidebar-item-state-color);
    }
    i {
        height: 24px !important;
        width: 24px !important;
        font-size: 24px;
        margin-bottom: 5px;
        background-size: 24px !important;
    }
    .navbar__item-spacer{
        display: none;
    }
    &.active{
        background-color: var(--sidebar-drawer-bg-color);
        border-top: 1px solid #414141;
        border-bottom: 1px solid #414141;
        border-left: 1px solid #414141;
        border-top-left-radius: 6px;
        border-bottom-left-radius: 6px;
        position:relative;
        &:before{
            content: '';
            width: 15px;
            height: 15px;
            border: 1px solid #414141;
            position: absolute;
            top: -15px;
            border-top: 0;
            border-right: 0;
            right: -3px;
            border-radius: 0 0 0 5px;
            transform: rotate(-90deg);
        }
        &:after{
            content: '';
            width: 15px;
            height: 15px;
            border: 1px solid #414141;
            position: absolute;
            bottom: -15px;
            border-top: 0;
            border-right: 0;
            right: -3px;
            border-radius: 0 0 0 5px;
            transform: rotate(180deg);
        }
        .navbar__item-spacer{
            display: block;
            position: absolute;
            background-color: var(--sidebar-drawer-bg-color);
            top: 0;
            bottom: 0;
            right: -3px;
            width: 10px;
            z-index: 9999;
            &:before{
                content: '';
                width: 15px;
                height: 15px;
                border: 3px solid var(--sidebar-drawer-bg-color);
                position: absolute;
                top: -12px;
                border-top: 0;
                border-right: 0;
                right: -3px;
                border-radius: 0 0 0 8px;
                transform: rotate(-90deg);
            }
            &:after{
                content: '';
                width: 15px;
                height: 15px;
                border: 3px solid var(--sidebar-drawer-bg-color);
                position: absolute;
                bottom: -12px;
                border-top: 0;
                border-right: 0;
                right: -3px;
                border-radius: 0 0 0 8px;
                transform: rotate(180deg);
            }
        }
    }
}

.navbar__container--icons {
    display: flex;
    flex-direction: row;
    width: 100%;
    flex-wrap: wrap;
    margin: 0;
    padding: 10px;
    align-items: flex-start;
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

@keyframes fadeInOpacity {
	0% {
		opacity: 0;
	}
	100% {
		opacity: 1;
	}
}
</style>