<template>
    <ui-socket
        section="job-browse-item"
        location="before-job-name"
        :socket-data="{ job }"
    />
    <ui-socket
        v-for="meta in job.meta"
        :location="`before-job-name:meta:${meta.name}`"
        section="job-browse-item"
        :socket-data="{ job, meta: meta.data }"
    />
    <a :href="jobLinkHref(job)" :data-job-id="job.uuid">
        {{ job.jobName }}
    </a>
        shortDescription
    }}</span>
    <ui-socket
        v-for="meta in job.meta"
        :location="`after-job-name:meta:${meta.name}`"
        section="job-browse-item"
        :socket-data="{ job, meta: meta.data }"
    />
    <ui-socket
        location="after-job-name"
        section="job-browse-item"
        :socket-data="{ job }"
    />
        <span class="text-secondary job-description" v-if="job.description">
</template>

<script lang="ts">
import { getRundeckContext } from "@/library";
import UiSocket from "@/library/components/utils/UiSocket.vue";
import { JobBrowseItem } from "@/library/types/jobs/JobBrowse";
import { defineComponent } from "vue";

const context = getRundeckContext();
export default defineComponent({
    name: "BrowserJobItem",
    components: { UiSocket },
    props: {
        job: {
            type: JobBrowseItem,
            required: true,
        },
    },
    methods: {
        jobLinkHref(job: JobBrowseItem) {
            return `${context.rdBase}project/${context.projectName}/job/show/${job.id}`;
        },
    },
    computed: {
        shortDescription() {
            if(!this.job.description){
              return ''
            }
            if(this.job.description.indexOf('\n')>-1){
              return this.job.description.split('\n')[0]
            }
            return this.job.description
        },
    },
});
</script>

<style scoped lang="scss">
.job-description {
    margin: 0 var(--spacing-2);
}
</style>