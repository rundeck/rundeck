<template>
    <template v-if="browsePath !== ''">
        <!-- todo: breadcrumb navigation -->
        <btn @click="rootBrowse('')">&larr;</btn>
        <i class="glyphicon glyphicon-folder-open"></i>
        {{ browsePath }}
    </template>
    <Browser
        :path="browsePath"
        :root="true"
        @tree_rootBrowse="rootBrowse"
    ></Browser>
    <modal
        id="bulk_confirm_modal"
        ref="bulk_confirm_modal"
        v-model="bulkConfirm"
        :title="$t('job.bulk.modify.confirm.panel.title')"
    >
        {{ $t(`job.bulk.${bulkConfirmAction}.confirm.message`) }}
        <template #footer>
            <btn @click="bulkConfirm = false">{{ $t("no") }}</btn>
            <btn type="danger" @click="deleteJobs">
                {{ $t(`job.bulk.${bulkConfirmAction}.button`) }}
            </btn>
        </template>
    </modal>
</template>

<script lang="ts">
import { getRundeckContext } from "@/library";
import {
    JobBrowserStore,
    JobBrowserStoreInjectionKey,
} from "@/library/stores/JobBrowser";
import { JobBrowseItem } from "@/library/types/jobs/JobBrowse";
import { defineComponent, inject, ref } from "vue";
import Browser from "./tree/Browser.vue";

const context = getRundeckContext();
const eventBus = context.eventBus;
export default defineComponent({
    name: "JobListPage",
    components: { Browser },
    setup(props) {
        return {
            jobBrowserStore: inject(
                JobBrowserStoreInjectionKey
            ) as JobBrowserStore,
            browsePath: ref(""),
            bulkConfirm: ref(false),
            bulkJobs: ref([]),
            bulkConfirmAction: ref(""),
        };
    },
    methods: {
        rootBrowse(path: string) {
            this.browsePath = path;
        },
        confirmAction(evt: { name: string; jobs: JobBrowseItem[] }) {
            const { name, jobs } = evt;
            this.bulkJobs = jobs;
            this.bulkConfirmAction = name;
            this.bulkConfirm = true;
        },
        deleteJobs() {
            // this.jobBrowserStore.deleteJobs(this.bulkJobs);
            this.bulkConfirm = false;
        },
    },
    mounted() {
        eventBus.on("job-action", this.confirmAction);
    },
});
</script>

<style scoped lang="scss"></style>
