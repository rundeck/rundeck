<template>
    <nav>
        <ul class="nav-bar__list" ref="list" v-if="navBar">
            <li>
                <ul class="nav-bar__list-group" ref="group-main">
                    <template v-for="item in navBar.groupItems('main')">
                        <NavBarItem v-if="item.type == 'link'" :item="item" :key="item.id" />
                        <NavBarContainer v-if="item.type == 'container'" :item="item" :key="item.id" />
                    </template>
                </ul>
            </li>
            <li style="margin-top: auto;width: 100%">
                <ul class="nav-bar__list-group nav-bar__list-group--bottom" ref="group-bottom">
                    <NavBarContainer v-if="navBar.isOverflowing" :item="navBar.overflowItem" />
                    <template v-for="item in navBar.groupItems('bottom')">
                        <NavBarItem v-if="item.type == 'link'" :item="item" :key="item.id" />
                        <NavBarContainer v-if="item.type == 'container'" :item="item" :key="item.id" />
                    </template>
                </ul>
            </li>
        </ul>
    </nav>
</template>

<script lang="ts">
import Vue from 'vue'
import {Component, Inject} from 'vue-property-decorator'
import {Observer} from 'mobx-vue'

import {NavBar} from '../../stores/NavBar'
import {RootStore} from '../../stores/RootStore'

import NavBarItem from './NavBarItem.vue'
import NavBarContainer from './NavBarContainer.vue'

@Observer
@Component({components: {
    NavBarItem,
    NavBarContainer
}})
export default class NavigationBar extends Vue {
    @Inject()
    private readonly rootStore!: RootStore

    navBar!: NavBar

    created() {
        this.navBar = this.rootStore.navBar
    }

    mounted() {
        console.log(this.$el)
        window.addEventListener('resize', this.overflow)
        /** After layout and before render handle overflow */
        window.requestAnimationFrame(this.overflow)
    }

    /**
     * Moves menu items into the "more" container until the navbar is no
     * longer overflowing.
     */
    overflow() {
        const el = this.$el as HTMLElement
        const list = this.$refs['list'] as HTMLElement

        const mainGroup = this.$refs['group-main'] as HTMLElement

        if (el.offsetHeight != el.scrollHeight) {
            this.navBar.overflowOne()
            /** 
             * Continue to force layout until no longer overflowing.
             * This provides for the least amount of flicker on page load.
             **/
            this.$forceUpdate()
            this.overflow()
        } else if (mainGroup.offsetTop + mainGroup.offsetHeight + 200 < el.offsetHeight) {
            this.navBar.showOne()
        }
    }
}
</script>

<style lang="scss" scoped>
nav {
    overflow-y: hidden;
    overflow-x: hidden;
    position: relative;
    height: 100%;
    background-color: #212120;

    scrollbar-color: transparent transparent;
    scrollbar-width: thin;

    &:hover {
        scrollbar-color: transparent slategray;
    }

    &::-webkit-scrollbar {
        background-color: transparent;
        width: 5px;
        height: 5px;
    }

    &:hover::-webkit-scrollbar {
        background-color: transparent;
        width: 5px;
        height: 5px;
    }

    &:hover::-webkit-scrollbar-thumb {
        background-color: slategray;
        width: 5px;
        height: 5px;
    }

    @media(max-width: 768px) {
        overflow-y: hidden;
        overflow-x: scroll;
    }
}

.nav-bar__list {
    list-style: none;
    height: 100%;
    display: flex;
    flex-direction: column;
    padding-left: 10px;
    padding-right: 10px;
    padding-bottom: 10px;
    padding-top: 10px;
    margin: 0;

    @media(max-width: 768px) {
        display: flex;
        height: 100%;
        padding-bottom: 5px;
    }
}

.nav-bar__list-group {
    height: 100%;
    display: flex;
    flex-direction: column;
    margin: 0;
    padding: 0;
}

.nav-bar__list-group--bottom {
    border-top-style: solid;
    border-top-width: 1px;
    border-top-color: #414141;
}

</style>