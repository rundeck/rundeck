<template>
    <nav>
        <ul ref="list" v-if="navBar">
            <template v-for="item in navBar.items">
                <NavBarItem v-if="item.type == 'link'" :item="item" :key="item.id" />
                <NavBarContainer v-if="item.type == 'container'" :item="item" :key="item.id" />
            </template>
            <NavBarContainer v-if="navBar.isOverflowing" :item="navBar.overflowItem" />
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
    }

    overflow() {
        const el = this.$el as HTMLElement
        const list = this.$refs['list'] as HTMLElement

        if (el.offsetHeight != el.scrollHeight) {
            this.navBar.overflowOne()
        } else if ((<HTMLElement>list.lastElementChild).offsetTop + 140 < el.offsetHeight) {
            this.navBar.showOne()
        }
    }
}
</script>

<style lang="scss" scoped>
nav {
    overflow-y: scroll;
    position: relative;
    height: 100%;
    background-color: #212120;

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

ul {
    padding-left: 10px;
    padding-right: 5px;
    padding-bottom: 10px;
    padding-top: 10px;
    margin: 0;

    @media(max-width: 768px) {
        display: flex;
        height: 100%;
        padding-bottom: 5px;
    }
}
</style>