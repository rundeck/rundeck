<template>
    <div id="utility-bar" class="utility-bar">
        <ul>
            <template v-for="item in utilityBar.containerGroupItems('root', 'left')">
                <UtilItem :item="item" :key="item.id" />
            </template>
            <!-- <li class="utility-bar__item" @click="handleClick">
                <i class="fas fa-newspaper fas-xs"/>
                <span>News</span>
                <Popper v-if="open" :open="true">
                    <div style="height: 200px; width: 200px" class="card">
                        Foo
                    </div>
                </Popper>
            </li> -->
        </ul>
        <ul style="flex-grow: 1; flex-direction: row-reverse;">
            <template v-for="item in utilityBar.containerGroupItems('root', 'right')">
                <UtilItem :item="item" :key="item.id" />
            </template>
        </ul>
    </div>
</template>


<script lang="ts">
import Vue from 'vue'
import {Component, Inject} from 'vue-property-decorator'
import {Observer} from 'mobx-vue'

import {UtilityBar} from '@/stores/UtilityBar'
import {RootStore} from '@/stores/RootStore'

import Popper from './Popper.vue'
import UtilItem from './UtilityBarItem.vue'
import ThemeSelectWidget from '@/components/widgets/theme-select/ThemeSelect.vue'

import {UtilityActionItem} from '@/stores/UtilityBar'

import { getAppLinks, getRundeckContext } from '@/rundeckService'


const appLinks = getAppLinks()
const rootStore = getRundeckContext().rootStore

rootStore.utilityBar.addItems([
] as Array<UtilityActionItem>)

@Observer
@Component({components: {
    Popper,
    UtilItem
}})
export default class UtilBar extends Vue {
    @Inject()
    private readonly rootStore!: RootStore

    utilityBar!: UtilityBar

    open = false

    created() {
        this.utilityBar = this.rootStore.utilityBar
    }

    mounted() {
    }

    handleClick() {
        this.open = !this.open
    }
}
</script>

