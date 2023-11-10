<template>
    <template v-if="browsePath !== ''">
        <!-- todo: breadcrumb navigation -->
        <btn @click="rootBrowse('')">&larr;</btn>
        <i class="glyphicon glyphicon-folder-open"></i>
        {{ browsePath }}
    </template>
    <JobBulkEditControls/>
    <Browser :path="browsePath"
             :root="true"
             @rootBrowse="rootBrowse"
             class="job_list_browser"
    />

</template>

<script lang="ts">
import JobBulkEditControls from '@/app/pages/job/browse/JobBulkEditControls.vue'
import { getRundeckContext } from "@/library";
import {
    JobBrowserStore,
    JobBrowserStoreInjectionKey,


} from "@/library/stores/JobBrowser";
import {JobPageStore, JobPageStoreInjectionKey} from '@/library/stores/JobPageStore'
import { defineComponent, inject, ref } from "vue";
import Browser from "./tree/Browser.vue";

const context = getRundeckContext();
const eventBus = context.eventBus;
export default defineComponent({
    name: "JobListPage",
    components: {JobBulkEditControls, Browser },
    setup(props) {
        const jobBrowserStore: JobBrowserStore = inject(
            JobBrowserStoreInjectionKey
        ) as JobBrowserStore;
        const jobPageStore: JobPageStore = inject(JobPageStoreInjectionKey) as JobPageStore
        return {
            jobBrowserStore,
            jobPageStore,
            browsePath: ref(""),
        };
    },
    methods: {
        rootBrowse(path: string) {
            this.browsePath = path;
        },
    },
    async mounted() {
      await this.jobPageStore.loadAuth()
    },
});
</script>

<style scoped lang="scss">
.job_list_browser{
  margin-top: var(--spacing-8)
}
</style>