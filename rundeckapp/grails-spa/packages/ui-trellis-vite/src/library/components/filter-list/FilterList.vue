<template>
    <div class="widget-wrapper">
        <div class="widget-section" style="flex-grow: 1; flex-shrink: 1;">
            <div>
                <div class="form-group form-group-sm has-feedback has-search">
                    <i class="fas fa-search form-control-feedback"/>
                    <input
                        ref="search"
                        type="text" 
                        class="filter-list__input form-control form-control-sm"
                        v-model="searchTerm"
                        :placeholder="searchText"/>
                </div>
            </div>
            <Skeleton :loading="loading">
                <RecycleScroller
                    ref="scroller"
                    :items="filtered()"
                    :item-size="itemSize"
                    :key="items.length"
                    v-slot="{ item }"
                    key-field="name"
                    class="scroller"
                >
                    <div style="height: 100%;" :ref="item[idField]" role="button" tabindex="0" class="scroller__item" :class="{'scroller__item--selected': item[idField] == selected}" @click="() => itemClicked(item)" @keypress.enter="itemClicked(item)">
                        <slot name="item" :item="item" scope="item"/>
                    </div>
                </RecycleScroller>
            </Skeleton>
        </div>
        <div class="widget-section" style="height: 40px; flex-grow: 0; flex-shrink: 0; padding-left: 10px">
            <slot name="footer"/>
            <!-- <a class="text-info" :href="allProjectsLink" @click@keypress.enter="handleSelect">View All Projects</a> -->
        </div>
    </div>
</template>

<script lang="ts">
import Vue from 'vue'
import {Component, Inject, Prop} from 'vue-property-decorator'
import { autorun } from 'mobx'
import {Observer} from 'mobx-vue'
import PerfectScrollbar from 'perfect-scrollbar'
import { RecycleScroller } from 'vue-virtual-scroller'
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'

import Skeleton from '../skeleton/Skeleton.vue'

RecycleScroller.updated = function() {
    if (!this.ps)
        this.$nextTick().then(() => {this.ps = new PerfectScrollbar(this.$el, {minScrollbarLength: 20})})
    else
        this.ps.update()
}

const destroy = RecycleScroller.beforeDestroy
RecycleScroller.beforeDestroy = function() {
    destroy.bind(this)()
    if (this.ps) {
        try {
            this.ps.destroy()
            this.ps = null
        } catch {}
    }
}

@Observer
@Component({components: {
    RecycleScroller,
    Skeleton
}})
export default class FilterList extends Vue {
    ps!: PerfectScrollbar

    searchTerm: string = ''

    @Prop({default: false})
    loading!: boolean

    @Prop({default: ''})
    searchText!: String

    @Prop()
    items!: Array<any>

    @Prop({default: 25})
    itemSize!: Number 

    @Prop({default: ''})
    selected!: string

    @Prop({default: 'id'})
    idField!: string

    filtered() {
        return this.items.filter(i => i.name.includes(this.searchTerm))
    }

    mounted() {
        autorun(() => {
            if (this.items.length) {
                /** May be necessary for virtual scroller to update */
                this.$forceUpdate()
            }
        })
        this.$nextTick().then(() => {
            (<HTMLElement>this.$refs['search']).focus()
        })
    }

    itemClicked(item: any) {
        (<HTMLElement>this.$refs[item[this.idField]]).blur()
        this.$emit('item:selected', item)
    }
}
</script>

<style scoped lang="scss">
.widget-wrapper {
    display: flex;
    flex-direction: column;
    justify-content: flex-start;
    height: 100%;
    max-width: 500px;
    overflow: hidden;
    min-height: 0;
}

.widget-section {
    display: flex;
    flex-direction: column;
    min-height: 0;
    justify-content: center;
}


.scroller {
    height: 100%;
    overflow-x: hidden;
    padding-right: 5px;
    flex-grow: 1;
}

.filter-list__input {
    border-width: 0.2em;
}

.scroller__item {
    position: relative;
    padding-left: 10px;
    width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    cursor: pointer;

    outline: none;

    &:hover::before, &:focus::before {
        position: absolute;
        content: "";
        height: 100%;
        border-left: 3px solid #F73F39;
        margin-left: -10px;
    }
}

.has-search .form-control-feedback {
    right: initial;
    left: 0;
    top: 8px;
}

.has-search .form-control {
    padding-right: 12px;
    padding-left: 34px;
}

.skeleton {
    --skel-color: #eeeeee !important;
    margin: 0 10px 0 10px;
}

</style>