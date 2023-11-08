<template>
    <btn
        type="success"
        size="xs"
        class="btn-simple btn-hover act_execute_job"
        :title="$t('action.prepareAndRun.tooltip')"
        :data-job-id="job.id"
        @click="$emit('run-job', job.id)"
        v-if="authz['run']"
    >
        <b class="glyphicon glyphicon-play"></b>
    </btn>
    <span
        :title="$t('disabled.job.run')"
        class="has_tooltip"
        data-toggle="tooltip"
        data-placement="auto bottom"
        v-else
    >
        <span class="btn btn-default btn-xs disabled">
            <b class="glyphicon glyphicon-play"></b>
        </span>
    </span>
</template>

<script lang="ts">
import { JobBrowseItem, JobBrowseMeta } from "@/library/types/jobs/JobBrowse";
import { defineComponent } from "vue";

export default defineComponent({
    name: "JobRunButton",
    props: {
        itemData: {
            type: Object,
            default: () => {},
        },
    },
    computed: {
        job(): JobBrowseItem {
            return this.itemData?.job;
        },
        authz(): Object | undefined {
            const data: any = this.job?.meta?.find(
                (meta: JobBrowseMeta) => meta.name === "authz"
            )?.data;
            return data?.authorizations||{}
        },
    },
});
</script>

<style scoped lang="scss"></style>