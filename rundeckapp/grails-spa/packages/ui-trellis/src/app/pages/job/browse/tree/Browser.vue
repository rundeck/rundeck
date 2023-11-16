<template>
    <div :class="!root ? 'subbrowse' : ''">
        <ul class="list-unstyled">
            <li v-for="item in sortedItems" v-if="items.length > 0" :key="item.job?item.job.id:item.groupPath">
                <browser-job-item :job="item" v-if="item.job" :key="item.job.id"/>
                <template v-else>
                    <browse-group-item
                      :item="item"
                      :expanded="isExpanded(item.groupPath)"
                      @toggleExpanded="toggle(item.groupPath)"
                      @rootBrowse="rootBrowse(item.groupPath)"
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
                    />
                </template>
            </li>
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
    JobBrowserStoreInjectionKey,
} from "@/library/stores/JobBrowser";
import {JobPageStore, JobPageStoreInjectionKey} from '@/library/stores/JobPageStore'
import { JobBrowseItem } from "@/library/types/jobs/JobBrowse";
import { defineComponent, inject, ref } from "vue";

const context = getRundeckContext();
const eventBus=context.eventBus
export default defineComponent({
    name: "Browser",
    components: { BrowseGroupItem, BrowserJobItem, UiSocket },
    props: {
        path: {
            type: String,
            default: "",
        },
        root: {
            type: Boolean,
            default: false,
        },
    },
    emits: ["rootBrowse"],
    setup(props) {
        const items = ref<JobBrowseItem[]>([]);
        return {
            jobBrowserStore: inject(
                JobBrowserStoreInjectionKey
            ) as JobBrowserStore,
            jobPageStore: inject(JobPageStoreInjectionKey) as JobPageStore,
            items,
            expandedItems: ref([]),
            wasExpanded: [],
            loading: ref(false),
            browsePath: props.path,
        };
    },
    computed: {
        sortedItems(): JobBrowseItem[] {
            //sort by group name, and job name, and job names come after groups
            if (!this.items || this.items.length < 1) {
                return [];
            }
            return this.items.sort((a: JobBrowseItem, b: JobBrowseItem) => {
                if (a.job && b.job) {
                    return a.jobName.localeCompare(b.jobName);
                }
                if (a.job) {
                    return 1;
                }
                if (b.job) {
                    return -1;
                }
                return a.groupPath.localeCompare(b.groupPath);
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
        selectAll(path:string){
          eventBus.emit(`job-bulk-edit-select-all-path`,path)
        },
        selectNone(path:string){
          eventBus.emit(`job-bulk-edit-select-none-path`,path)
        },
        async rootBrowse(path: string) {
            this.$emit("rootBrowse", path);
        },
        async refresh() {
            this.loading = true;
            this.items = await this.jobBrowserStore.loadItems(this.browsePath);
            this.loading = false;
        },
    },
    watch:{
      path(){
        this.browsePath=this.path
        this.refresh()
      }
    },
    async mounted() {
        await this.refresh();
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
</style>