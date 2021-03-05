<template>
    <div class="widget-wrapper" @foo="alert('Foo')">
        <div class="widget-section" style="flex-grow: 1; flex-shrink: 1;">
            <div style="padding: 10px 10px 0 10px;">
                <div class="form-group form-group-sm has-feedback has-search">
                    <i class="fas fa-search form-control-feedback"/>
                    <input 
                        type="text" 
                        class="form-control form-control-sm"
                        v-model="searchTerm"
                        placeholder="Search all projects"/>
                </div>
            </div>
            <RecycleScroller @foo="alert('Foo')"
                ref="scroller"
                :items="projects.search(searchTerm)"
                :item-size="25"
                :key="projects.search(searchTerm).length"
                v-slot="{ item }"
                key-field="name"
                class="scroller"
            >
                <div class="scroller__item" :title="item.name" @click="itemClicked(item)">
                    <span>{{item.label || item.name}}</span>
                </div>
            </RecycleScroller>
        </div>
        <div class="widget-section" style="height: 40px; flex-grow: 0; flex-shrink: 0;border-top: solid 1px grey; padding-left: 10px">
            <a href="/">View All Projects</a>
        </div>
    </div>
</template>

<script lang="ts">
import Vue from 'vue'
import {Component, Inject} from 'vue-property-decorator'
import {Observer} from 'mobx-vue'

import { RecycleScroller } from 'vue-virtual-scroller'
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'

import {RootStore} from '../../../stores/RootStore'

import { ProjectStore, Project } from '../../../stores/Projects'

import PerfectScrollbar from 'perfect-scrollbar'

import { autorun } from 'mobx'


RecycleScroller.updated = function() {
    if (!this.ps)
        this.$nextTick().then(() => {this.ps = new PerfectScrollbar(this.$el, {minScrollbarLength: 20})})
    else
        this.ps.update()
}

const destroy = RecycleScroller.beforeDestroy
RecycleScroller.beforeDestroy = function() {
    destroy.bind(this)()
    this.ps.destroy()
    this.ps = null
}

@Observer
@Component({components: {
    RecycleScroller
}})
export default class ProjectSelect extends Vue {
    @Inject()
    private readonly rootStore!: RootStore

    projects!: ProjectStore

    ps!: PerfectScrollbar

    searchTerm: string = ''

    created() {
        this.projects = this.rootStore.projects
        this.projects.load()
    }

    mounted() {
        autorun(() => {
            if (this.projects.projects.length) {
                console.log('Update')
                this.$forceUpdate()
            }
        })
        
    }

    itemClicked(project: Project) {
        console.log(project.name)
        this.$emit('project:selected', project)
    }
}
</script>

<style scoped lang="scss">
.widget-wrapper {
    display: flex;
    flex-direction: column;
    height: 400px;
    width: 500px;
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
}

.scroller__item {
    position: relative;
    padding-left: 10px;
    width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    cursor: pointer;

    &:hover::before {
        position: absolute;
        content: "";
        height: 100%;
        border-left: 3px solid red;
        margin-left: -10px;
    }
}

.has-search .form-control-feedback {
    right: initial;
    left: 0;
    top: 8px;
    color:black;
}

.has-search .form-control {
    padding-right: 12px;
    padding-left: 34px;
}
</style>