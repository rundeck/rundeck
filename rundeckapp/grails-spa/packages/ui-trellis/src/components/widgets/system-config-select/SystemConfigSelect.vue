<template>
    <FilterList v-on="$listeners" :items="categories" id-field="name" :selected="selected" searchText="Filter System Configurations" :itemSize="40">
        <template v-slot:item="{item}">
            <SystemConfigSelectItem :category="item"/>
        </template>
    </FilterList>
</template>


<script lang="ts">
import Vue from 'vue'

import {Component, Prop} from 'vue-property-decorator'
import {Observer} from 'mobx-vue'

import FilterList from '../../filter-list/FilterList.vue'

import SystemConfigSelectItem from './SystemConfigSelectItem.vue'

@Observer
@Component({components: {FilterList, SystemConfigSelectItem}})
export default class SystemConfigSelect extends Vue {
    @Prop()
    categories!: {[k:string]:any}[]

    @Prop({default: ''})
    selected!: string

    created() {
        console.log(`Created SysConfig select, nestedCategories: ${JSON.stringify(this.categories)}`)
    }

    itemUpdated() {
        console.log(`Updated SysConfig select, nestedCategories: ${JSON.stringify(this.categories)}`)
    }
}
</script>

<style scoped lang="scss">
::v-deep .plugin-icon {
    height: 20px !important;
    width: 20px !important;
}

::v-deep .scroller__item {
    border-radius: 5px;
    padding-left: 10px;

    &--selected, &:hover {
        background-color: #0080ff;
    }

    &:before {
        content: none !important;
    }
}
</style>