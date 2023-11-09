<template>
    <span v-if="jobPageStore.bulkEditMode">
        <input type="checkbox" v-model="selected" class="checkbox-inline" />
    </span>
</template>

<script lang="ts">
import {
    JobPageStore,
    JobPageStoreInjectionKey,
} from "@/library/stores/JobBrowser";
import { JobBrowseItem, JobBrowseMeta } from "@/library/types/jobs/JobBrowse";
import { defineComponent, inject, ref } from "vue";

export default defineComponent({
    name: "BulkSelectCheckbox",
    props: {
        itemData: {
            type: Object,
            default: () => {},
        },
    },
    setup() {
        return {
            jobPageStore: inject(JobPageStoreInjectionKey) as JobPageStore,
            selected: ref(false),
        };
    },
    watch: {
        selected(val: boolean) {
          if(val){
            this.jobPageStore.addBulkJob(this.job)
          }else{
            this.jobPageStore.removeBulkJob(this.job)
          }
        },
      'jobPageStore.selectedJobs': {
        handler(val: JobBrowseItem[]) {
          this.selected = val.find((job: JobBrowseItem) => job.id === this.job.id) !== undefined
        },
        immediate: true
      }
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
    },
});
</script>

<style scoped lang="scss"></style>
