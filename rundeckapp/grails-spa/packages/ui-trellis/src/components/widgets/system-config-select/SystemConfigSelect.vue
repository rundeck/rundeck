<template>
    <FilterList v-on="$listeners" :items="correctedCategories()" id-field="name" :selected="selected" searchText="Filter System Configurations" :itemSize="40">
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
    // prop is opened to any type until we can change SystemConfiguration.vue payload to match
    // the format FilterList supports
    @Prop()
    categories!: any

    @Prop({default: ''})
    selected!: string

    itemUpdated() {
        console.log(`Updated SysConfig select, nestedCategories: ${JSON.stringify(this.categories)}`)
    }

    correctedCategories() {
        // format current SystemConfiguration.vue category payload to match needed FilterList props
        if (!Array.isArray(this.categories)) {
            return Object.entries(this.categories).map(
                (e) => ( { name: e[0], idField: e[0], configs: e[1] } )
            );
        } else {
            return this.categories;
        }
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