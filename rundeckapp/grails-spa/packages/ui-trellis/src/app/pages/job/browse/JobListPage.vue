<template>
    <JobBulkEditControls />
    <Browser
        :path="browsePath"
        :root="true"
        @rootBrowse="rootBrowse"
        class="job_list_browser"
        :expand-level="jobPageStore.groupExpandLevel"
        :query-refresh="queryRefresh"
        v-if="loaded"
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
import {
    JobPageStore,
    JobPageStoreInjectionKey,
} from "@/library/stores/JobPageStore";
import { defineComponent, inject, ref } from "vue";
import Browser from "./tree/Browser.vue";
const eventBus = getRundeckContext().eventBus;
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
        const jobPageStore: JobPageStore = inject(
            JobPageStoreInjectionKey
        ) as JobPageStore;
        return {
            jobBrowserStore,
            jobPageStore,
            browsePath: ref(""),
            loaded: ref(false),
            queryRefresh: ref(false),
        };
    },
    methods: {
        rootBrowse(path: string) {
            //deselect any jobs
            this.jobPageStore.selectedJobs = [];
            this.browsePath = path;
            eventBus.emit("job-list-page:browsed", path);
            //todo: browser history push
        },
    },
    async mounted() {
        await this.jobPageStore.load();
        this.loaded = true;
        eventBus.on("job-list-page:search", async () => {
            this.queryRefresh = !this.queryRefresh;
            await this.jobBrowserStore.reload();
        });
        eventBus.on("job-list-page:rootBrowse", async (path:string) => {
            this.rootBrowse(path)
        });
    },
});
</script>

<style scoped lang="scss">
.empty-splash {
    .btn + .btn {
        margin-left: var(--spacing-4);
    }
}
</style>