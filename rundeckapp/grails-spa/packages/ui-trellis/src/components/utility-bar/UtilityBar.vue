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

import {UtilityBar} from '../../stores/UtilityBar'
import {RootStore} from '../../stores/RootStore'

import Popper from './Popper.vue'
import UtilItem from './UtilityBarItem.vue'

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

<style scoped lang="scss">
.utility-bar {
    position: relative;
    background-color: white;
    box-shadow: 0px -1px grey;
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    align-content: center;
}

ul {
    height: 100%;
    display: flex;
    align-content: center;
    align-items: center;
    list-style: none;
    margin: 0;
    padding: 0px 10px;
}

</style>

<style lang="scss">
.utility-bar__item {
    display: flex;
    align-items: center;
    height: 100%;
    margin: 0;
    color: #808080;
    padding: 2px 5px;
    cursor: pointer;

    &:hover {
        color: #373737;
        background-color:#E5E5E5;
    }

    >span {
        margin-left: 5px;
        font-weight: 700;
    }

    & &-icon.rdicon {
        background-size: 14px 14px;
        height: 14px;
        width: 14px;
    }

    &-counter {
        height: 100%;
        min-width: 19px;
        padding: 0 5px;
        border-radius: 50%;
        background-color: #808080;;
        text-align: center;
        color: white;
        font-size: 12px;
    }

    &:hover &-counter {
        background-color: #373737;
    }
}
</style>