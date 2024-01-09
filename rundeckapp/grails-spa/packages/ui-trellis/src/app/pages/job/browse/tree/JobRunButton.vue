<template>
  <template v-if="!jobPageStore.bulkEditMode">
    <template v-if="jobEnabled && runAuthorized">
        <btn
            type="success"
            size="xs"
            class="btn-simple btn-hover act_execute_job"
            :title="$t('action.prepareAndRun.tooltip')"
            :data-job-id="job.id"
            @click="$emit('run-job', job.id)"
            v-if="projEnabled"
        >
            <b class="glyphicon glyphicon-play"></b>
        </btn>
        <btn
            v-else
            :v-tooltip.hover="$t('disabled.job.run')"
            disabled
            size="xs"
            type="simple"
        >
            <b class="glyphicon glyphicon-play"></b>
        </btn>
    </template>
    <template v-else>
        <span
            class="text-muted disabled"
            style="padding: 4px 5px"
            :title="$t('cannot.run.job')"
            disabled
        >
            <b class="glyphicon glyphicon-minus"></b>
        </span>
    </template>
  </template>
</template>

<script lang="ts">
import {JobPageStore, JobPageStoreInjectionKey} from '@/library/stores/JobPageStore'
import { JobBrowseItem, JobBrowseMeta } from "@/library/types/jobs/JobBrowse";
import { defineComponent, inject } from "vue";

export default defineComponent({
    name: "JobRunButton",
    props: {
        itemData: {
            type: Object,
            default: () => {},
        },
    },
    setup() {
        return {
            jobPageStore: inject(JobPageStoreInjectionKey) as JobPageStore,
        };
    },
    computed: {
        job(): JobBrowseItem {
            return this.itemData?.job;
        },
        authz(): Object | undefined {
            const data: any = this.job?.meta?.find(
                (meta: JobBrowseMeta) => meta.name === "authz"
            )?.data;
            return data || {};
        },
        runAuthorized(): boolean {
            return !!this.authz?.run;
        },
        jobEnabled(): boolean {
            const data: any = this.job?.meta?.find(
                (meta: JobBrowseMeta) => meta.name === "schedule"
            )?.data;
            return data?.executionEnabled || false;
        },
        projEnabled(): boolean {
            return (
                this.jobPageStore.projectExecutionsEnabled &&
                this.jobPageStore.executionMode
            );
        },
    },
});
</script>

<style scoped lang="scss">
.btn,span {
    margin-right: var(--spacing-2);
}
</style>