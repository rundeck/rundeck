<template>
    <nav>
        <ul v-if="rootStore">
            <li v-for="item in rootStore.navBar.items" :key="item.id">
                {{item.label}}
            </li>
        </ul>
    </nav>
</template>

<script lang="ts">
import Vue from 'vue'
import {Inject} from 'vue-property-decorator'

import {} from 'mobx'
import {Observer} from 'mobx-vue'

import {RootStore} from '../../stores/RootStore'

@Observer
export default class NavBar extends Vue {
    @Inject()
    private readonly rootStore!: RootStore

    created() {

    }
}
</script>