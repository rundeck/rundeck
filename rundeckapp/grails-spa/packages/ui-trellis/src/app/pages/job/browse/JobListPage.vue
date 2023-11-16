<template>
    <JobBulkEditControls />
    <template v-if="browsePath !== ''">
        <!-- todo: breadcrumb navigation -->
        <btn @click="rootBrowse('')">&larr;</btn>
        <i class="glyphicon glyphicon-folder-open"></i>
        {{ browsePath }}
    </template>
    <Browser
        :path="browsePath"
        :root="true"
        @rootBrowse="rootBrowse"
        class="job_list_browser"
    >
        <ui-socket section="job-list-page" location="empty-splash">
          <div class="empty-splash">
          <create-new-job-button btn-type="cta">
            {{ $t("job.create.button") }}
          </create-new-job-button>
          <upload-job-button></upload-job-button>
          </div>
        </ui-socket>
    </Browser>
</template>

<script lang="ts">
import CreateNewJobButton from "@/app/pages/job/browse/components/CreateNewJobButton.vue";
import UploadJobButton from "@/app/pages/job/browse/components/UploadJobButton.vue";
import JobBulkEditControls from "@/app/pages/job/browse/JobBulkEditControls.vue";
import { getRundeckContext } from "@/library";
import UiSocket from "@/library/components/utils/UiSocket.vue";
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
    components: {
        UploadJobButton,
        UiSocket,
        CreateNewJobButton,
        JobBulkEditControls,
        Browser,
    },
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
            //deselect any jobs
            this.jobPageStore.selectedJobs = []
            this.browsePath = path;
        },
    },
    async mounted() {
      await this.jobPageStore.loadAuth()
    },
});
</script>

<style scoped lang="scss">
.job_list_browser {
    margin-top: var(--spacing-8);
}
.empty-splash{
  .btn+.btn{
    margin-left: var(--spacing-4);
  }
}
</style>