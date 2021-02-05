<template>
    <nav>
        <ul v-if="navBar.get()">
            <li v-for="item of navBar.items" :key="item.id">
                {{item.label}}
            </li>
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

@Observer
@Component
export default class NavigationBar extends Vue {
    @Inject()
    private readonly rootStore!: RootStore

    navBar: IObservableValue<NavBar|undefined> = observable.box(undefined)

    mounted() {
        this.navBar.set(this.rootStore.navBar)
    }
}
</script>