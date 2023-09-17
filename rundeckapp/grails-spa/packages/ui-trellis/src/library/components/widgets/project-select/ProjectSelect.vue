<template>
    <div class="widget-wrapper">
        <div class="widget-section" style="flex-grow: 1; flex-shrink: 1;">
            <div style="padding: 10px 10px 0 10px;">
                <div class="form-group form-group-sm has-feedback has-search">
                    <i class="fas fa-search form-control-feedback"/>
                    <input
                        ref="search"
                        type="text" 
                        class="form-control form-control-sm"
                        v-model="searchTerm"
                        placeholder="Search all projects"/>
                </div>
            </div>
            <Skeleton :loading="!projectStore.loaded">
                <RecycleScroller
                    ref="scroller"
                    :items="projectStore.search(searchTerm)"
                    :item-size="25"
                    :key="projectStore.search(searchTerm).length"
                    v-slot:default="{ item }"
                    key-field="name"
                    class="scroller"
                >
                    <a role="button" tabindex="0" class="scroller__item" :title="item.name"
                       :href="itemHref(item)">
                        <span class="text-ellipsis">{{item.label || item.name}}<span v-if="searchTerm && item.label && item.label!==item.name" class="text-muted"> {{item.name}}</span></span>
                    </a>
                </RecycleScroller>
            </Skeleton>
        </div>
             <div class="btn-group btn-group-justified" style="height: 40px; flex-grow: 0; flex-shrink: 0; border-top: solid 1px grey;">
                <a :href="allProjectsLink" role="button" tabindex="0" class="btn btn-default scroller__subbutton" style="border-radius: 0px; border: 0px; border-right: solid 1px grey;">
                <i class="far fa-eye"></i>
                View All</a>
                <a :href="createProjectLink" role="button" tabindex="0" class="btn btn-default scroller__subbutton" style="border-radius: 0px; border: 0px;">
                <i class="fas fa-plus-circle"></i>
                Create Project</a>
        </div>
    </div>
</template>

<script lang="ts">
import { ref, nextTick, defineComponent} from 'vue'
import PerfectScrollbar from 'perfect-scrollbar'
import { RecycleScroller } from 'vue-virtual-scroller'
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'

import {getAppLinks} from '../../../rundeckService'
import Skeleton from '../../skeleton/Skeleton.vue'
import {url} from '../../../rundeckService'
import {Project} from "../../../stores/Projects";

const ps = ref<PerfectScrollbar>()
RecycleScroller.updated = function() {
    if (!ps.value)
        nextTick().then(() => {ps.value = new PerfectScrollbar(this.$el, {minScrollbarLength: 20})})
    else
        ps.value.update()
}

const unmount = RecycleScroller.beforeUnmount
RecycleScroller.beforeUnmount = function() {
    unmount.bind(this)()
    if (ps.value) {
        try {
            ps.value.destroy()
            ps.value = null
        } catch {}
    }
}

export default defineComponent({
  name:"ProjectSelect",
  components: {
    Skeleton,
    RecycleScroller
  },
  data() {
      return {
          projectStore : window._rundeck.rootStore.projects,
          searchTerm : ''
      }
  },
  computed: {
      allProjectsLink() {
          return getAppLinks().menuHome
      },
      createProjectLink() {
          return url('resources/createProject').href
      }
  },
  methods: {
      itemHref(project: Project){
          return url(`?project=${project.name}`).href
      }
  },
  beforeMount() {
      this.projectStore.load()
  },
  mounted() {
      if (this.projectStore.projects.length) {
          /** May be necessary for virtual scroller to update */
          this.proxy?.$forceUpdate()
      }
      nextTick().then(() => {
          (<HTMLElement>this.$refs['search']).focus()
      })
  }
})
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
        border-left: 3px solid var(--brand-color);
        margin-left: -10px;
    }
}

a.scroller__item {
    color: var(--font-color);
    display: block;

    &:hover {
        text-decoration: none;
    }
}

a.scroller__subbutton:focus {
    text-decoration: underline;
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
.text-ellipsis{
    text-overflow: ellipsis;
    overflow: hidden;
    white-space: nowrap;
}
</style>