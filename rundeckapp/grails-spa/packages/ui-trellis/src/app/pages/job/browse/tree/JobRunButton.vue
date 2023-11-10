<template>
    <btn
        type="success"
        size="xs"
        class="btn-simple btn-hover act_execute_job"
        :title="$t('action.prepareAndRun.tooltip')"
        :data-job-id="job.id"
        @click="$emit('run-job', job.id)"
        v-if="canRun"
    >
        <b class="glyphicon glyphicon-play"></b>
    </btn>
    <btn  v-else :v-tooltip.hover="$t('disabled.job.run')" disabled size="xs" type="simple">
          <b class="glyphicon glyphicon-play"></b>
    </btn>
</template>

<script lang="ts">
import {
    JobPageStore,
    JobPageStoreInjectionKey,
} from "@/library/stores/JobBrowser";
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
            return data?.authorizations || {};
        },
        canRun(): boolean {
            const runAuth = this.authz?.run || false;
            const projEnabled =
                this.jobPageStore.projectExecutionsEnabled &&
                this.jobPageStore.executionMode;
            return runAuth && projEnabled;
        },
    },
});
</script>

<style scoped lang="scss">
.btn,span {
    margin-right: var(--spacing-2);
}
</style>