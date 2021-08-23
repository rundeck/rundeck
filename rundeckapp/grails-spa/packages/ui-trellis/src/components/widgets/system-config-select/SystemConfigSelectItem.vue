<template>
    <div v-bind:class="[category.isSubcat ? 'subcategory' : 'category', 'system-config-select__item']">
        <span v-if="!category.isSubcat" class="fas fa-angle-right"/>
        <span class="title">{{category.name}}</span>
    </div>
</template>

<script lang="ts">
import Vue from 'vue'

import {Component, Inject, Prop} from 'vue-property-decorator'
import {Observer} from 'mobx-vue'

import PluginInfo from '../../plugins/PluginInfo.vue'


@Observer
@Component({components: {PluginInfo}})
export default class SystemConfigSelectItem extends Vue {
    @Prop()
    category!: string

    @Prop({default: false})
    isSubcat!: boolean
}
</script>

<style scoped lang="scss">
::v-deep .plugin-icon {
    border-radius: 4px;
    height: 20px;
    width: 20px;
}

.category {
    height: 40px;
    display: flex;
    align-items: center;

    .title {
    margin-left: 10px;
    font-weight: 600;
    }
}

.subcategory {
    height: 30px;
    display: flex;
    align-items: center;
    
    .title {
    margin-left: 15px;
    font-weight: 300;
    }
}
</style>