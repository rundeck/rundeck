<template>
    <nav>
        <ul v-if="navBar.get()">
            <NavBarItem v-for="item in navBar.get().items" :item="item" :key="item.id" />
        </ul>
    </nav>
</template>

<script lang="ts">
import Vue from 'vue'
import {Component, Inject} from 'vue-property-decorator'

import {IObservableValue, observable} from 'mobx'
import {Observer} from 'mobx-vue'

import {NavBar} from '../../stores/NavBar'
import {RootStore} from '../../stores/RootStore'

import NavBarItem from './NavBarItem.vue'

@Observer
@Component({components: {
    NavBarItem
}})
export default class NavigationBar extends Vue {
    @Inject()
    private readonly rootStore!: RootStore

    navBar: IObservableValue<NavBar|undefined> = observable.box(undefined)

    mounted() {
        this.navBar.set(this.rootStore.navBar)
    }
}
</script>

<style lang="scss" scoped>
nav {
    overflow-y: scroll;
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