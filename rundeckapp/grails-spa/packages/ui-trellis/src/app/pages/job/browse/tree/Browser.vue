<template>
    <div :class="!root ? 'subbrowse' : ''">

        <template v-if="!loading && breakpointHit">
          <p class="breakpoint-info">
            {{$t('job.tree.breakpoint.hit.info')}}
            <btn @click="loadMeta(this.browsePath)"  size="xs">
              {{$t('job.tree.breakpoint.load.button.title')}}
            </btn>
          </p>

        </template>
        <ul class="list-unstyled">
            <li v-for="item in sortedGroups" v-if="sortedGroups.length > 0" :key="item.groupPath">
                <browse-group-item
                  :item="item"
                  :expanded="isExpanded(item.groupPath)"
                  @toggleExpanded="toggle(item.groupPath)"
                  @rootBrowse="rootBrowse(item.groupPath)"
                  :href="this.jobPageStore.jobPagePathHref(item.groupPath)"
                  :key="item.groupPath"
                >
                  <template v-if="jobPageStore.bulkEditMode && isExpanded(item.groupPath)" #supplemental >
                    <btn
                      size="xs"
                      type="simple"
                      class="btn-hover visibility-hidden button-spacing"
                      @click="selectAll(item.groupPath)"
                    >
                      <b class="glyphicon glyphicon-check"></b>
                      {{ $t("select.all") }}
                    </btn>
                    <btn
                      size="xs"
                      type="simple"
                      class="btn-hover visibility-hidden"
                      @click="selectNone(item.groupPath)"
                    >
                      <b class="glyphicon glyphicon-unchecked"></b>
                      {{ $t("select.none") }}
                    </btn>
                  </template>
                </browse-group-item>
                <Browser
                  :path="item.groupPath"
                  v-if="isExpanded(item.groupPath)"
                  @rootBrowse="rootBrowse"
                  @empty="childGroupEmpty(item)"
                  :key="item.groupPath"
                  :expand-level="expandLevel-1"
                  :query-refresh="queryRefresh"
                />
            </li>
              <RecycleScroller
                  ref="scroller"
                  :items="sortedItems"
                  :item-size="27"
                  v-slot:default="{ item,active }"
                  key-field="id"
                  itemTag="li"
                  page-mode
                  :key="browsePath"
              >
                <browser-job-item :job="item" v-if="item.job" :load-meta="breakpointHit"/>
              </RecycleScroller>

            <li v-if="items.length === 0">
                <template v-if="loading">
                  <i class="fas fa-spinner fa-pulse" ></i>
                </template>
                <slot v-else/>
            </li>
        </ul>
    </div>
</template>

<script lang="ts">
import BrowseGroupItem from "@/app/pages/job/browse/tree/BrowseGroupItem.vue";
import BrowserJobItem from "@/app/pages/job/browse/tree/BrowserJobItem.vue";
import { getRundeckContext } from "@/library";
import UiSocket from "@/library/components/utils/UiSocket.vue";
import {
  JobBrowserStore,
  JobBrowserStoreInjectionKey, JobBrowserStoreItem,
} from '@/library/stores/JobBrowser'
import { JobPageFilter, JobPageStore, JobPageStoreInjectionKey} from '@/library/stores/JobPageStore'
import { JobBrowseItem } from "@/library/types/jobs/JobBrowse";
import { defineComponent, inject, ref } from "vue";
import {RecycleScroller} from 'vue-virtual-scroller';
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'

const context = getRundeckContext();
const eventBus=context.eventBus
export default defineComponent({
    name: "Browser",
    components: {RecycleScroller, BrowseGroupItem, BrowserJobItem, UiSocket },
    props: {
        path: {
            type: String,
            default: "",
        },
        root: {
            type: Boolean,
            default: false,
        },
        expandLevel: {
          type: Number,
          default: 0,
        },
        queryRefresh:{
            type: Boolean,
            default: false,
        }
    },
    emits: ["rootBrowse",'empty'],
    setup(props) {
        const items = ref<JobBrowseItem[]>([]);
        return {
            jobBrowserStore: inject(
                JobBrowserStoreInjectionKey
            ) as JobBrowserStore,
            jobPageStore: inject(JobPageStoreInjectionKey) as JobPageStore,
            items,
            breakpointHit:ref(false),
            expandedItems: ref([]),
            wasExpanded: [],
            loading: ref(false),
            browsePath: props.path,
            subs:{}
        };
    },
    computed: {
        sortedGroups(): JobBrowseItem[] {
            //sort by group name, and job name, and job names come after groups
            if (!this.items || this.items.length < 1) {
                return [];
            }
            return this.items.filter((a:JobBrowseItem)=>!a.job).sort((a: JobBrowseItem, b: JobBrowseItem) => {
                return a.groupPath.localeCompare(b.groupPath);
            });
        },
        sortedItems(): JobBrowseItem[] {
            //sort by group name, and job name, and job names come after groups
            if (!this.items || this.items.length < 1) {
                return [];
            }
            let filters:JobPageFilter[] = this.jobPageStore.filters||[]
            return this.items.filter((a:JobBrowserStoreItem)=>a.job)
              .filter((a:JobBrowserStoreItem)=>filters.every((f)=>f.filter(a)))
              .sort((a: JobBrowseItem, b: JobBrowseItem) => {
                  return a.jobName.localeCompare(b.jobName);
            });
        },
    },
    methods: {
        isExpanded(path: string) {
            return this.expandedItems.indexOf(path) >= 0;
        },
        toggle(path: string) {
            if (this.isExpanded(path)) {
                this.expandedItems = this.expandedItems.filter(
                    (i) => i !== path
                );
            } else {
                this.expandedItems.push(path);
            }
        },
        childGroupEmpty(item:JobBrowseItem){
          //remove
          this.items = this.items.filter(
            (i) => i.job ||(i.groupPath !== item.groupPath)
          );
          if(this.items.length<1){
            this.$emit('empty')
          }
        },
        async loadMeta(path:string){
          let item = this.jobBrowserStore.findPath(this.browsePath)
          item.breakpoint=0
          item.loaded=false
          await this.refresh()
        },
        selectAll(path:string){
          eventBus.emit(`job-bulk-edit-select-all-path`,path)
        },
        selectNone(path:string){
          eventBus.emit(`job-bulk-edit-select-none-path`,path)
        },
        async rootBrowse(path: string) {
            this.$emit("rootBrowse", path, this.jobPageStore.jobPagePathHref(path));
        },
        async refresh(initial:boolean=false) {
            this.loading = true;
            this.items = await this.jobBrowserStore.loadItems(this.browsePath);
            this.breakpointHit = this.jobBrowserStore.findPath(this.browsePath).bpHit
            this.loading = false;
            if(initial){
              //expand children if expand level is greater than 0
              if(this.expandLevel>0){
                this.items.forEach((item)=>{
                  if(item.groupPath ){
                    this.expandedItems.push(item.groupPath)
                  }
                })
              }
            }
            if(this.items.length<1){
              this.$emit('empty')
            }
        },
        modifiedPaths(paths:string[]){
          if(paths.includes(this.browsePath)){
            this.refresh()
          }
        }
    },
    watch:{
      path(){
        if(this.browsePath===this.path){
          return
        }
        this.browsePath=this.path
        this.refresh()
      },
      queryRefresh(){
        this.refresh(true)
      }
    },
    async mounted() {
        this.subs['job-bulk-modified-paths'] = (paths:string[])=>{this.modifiedPaths(paths)}
        eventBus.on(`job-bulk-modified-paths`,this.subs['job-bulk-modified-paths'])
        await this.refresh(true);
    },
    beforeUnmount() {
      eventBus.off(`job-bulk-modified-paths`,this.subs['job-bulk-modified-paths'])
    },
});
</script>

<style scoped lang="scss">
.subbrowse {
    padding-left: 20px;
}
.btn.button-spacing{
  margin-right: var(--spacing-2);
}
.breakpoint-info{
  color: var(--text-secondary-color)
}
</style>