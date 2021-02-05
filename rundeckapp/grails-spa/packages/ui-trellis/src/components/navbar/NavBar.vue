<template>
    <nav>
        <ul v-if="navBar.get()">
            <li v-for="item in navBar.items" :key="item.id">
                {{item.label}}
            </li>
        </ul>
    </nav>
</template>

<script lang="ts">
import Vue from 'vue'
import {Inject} from 'vue-property-decorator'

import {IObservableValue, observable} from 'mobx'
import {Observer} from 'mobx-vue'

import {NavBar} from '../../stores/NavBar'
import {RootStore} from '../../stores/RootStore'

@Observer
export default class NavigationBar extends Vue {
    @Inject()
    private readonly rootStore!: RootStore

    navBar: IObservableValue<NavBar|undefined> = observable.box(undefined)

    created() {
        this.navBar.set(this.rootStore.navBar)
    }
}
</script>