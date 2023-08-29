<template>
    <div id="utility-bar" class="utility-bar">
        <ul>
            <template v-for="item in barContainerRootLeftItems" :key="item.id">
                <UtilItem :item="item" />
            </template>
        </ul>
        <ul style="flex-grow: 1; flex-direction: row-reverse;">
            <template v-for="item in barContainerRootRightItems" :key="item.id" >
                <UtilItem :item="item"/>
            </template>
        </ul>
    </div>
</template>


<script lang="ts">
import {defineComponent} from 'vue'
import type {PropType} from "vue"
import UtilItem from './UtilityBarItem.vue'

export default defineComponent({
    name:"UtilityBar",
    components: {
        UtilItem
    },
    data() {
        return {
            open: false,
            utilityBar: window._rundeck.rootStore.utilityBar
        }
    },
    computed: {
      barContainerRootLeftItems() {
        return this.utilityBar.containerGroupItems('root', 'left')
      },
      barContainerRootRightItems() {
        return this.utilityBar.containerGroupItems('root', 'right')
      }
    },
    methods: {
        handleClick() {
            this.open = !this.open
        }
    }
})
</script>

<style scoped lang="scss">
.utility-bar {
    position: relative;
    background-color: var(--utility-bar-background-color);
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
    // color: #808080;
    color: var(--font-color);
    padding: 2px 5px;
    cursor: pointer;

    &:hover {
        color: var(--font-color);
        background-color: var(--background-color-accent-lvl2);
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
        // background-color: #808080;
        background-color: var(--font-color);
        text-align: center;
        color: white;
        font-size: 12px;
    }

    &:hover &-counter {
        background-color: var(--font-color);
    }
}
</style>