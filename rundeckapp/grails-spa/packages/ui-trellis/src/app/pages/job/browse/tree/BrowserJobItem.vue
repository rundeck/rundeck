<template>
    <div
        class="job_list_row hover-reveal-hidden"
        @click="handleClick"
        ref="itemDiv"
    >
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
        <a :href="jobLinkHref(job)" :data-job-id="job.uuid" class="link-quiet">
            {{ job.jobName }}
        </a>
        <span class="text-secondary job-description" v-if="job.description">
            {{ shortDescription }}
        </span>
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
    </div>
</template>

<script lang="ts">
import { getRundeckContext } from "@/library";
import UiSocket from "@/library/components/utils/UiSocket.vue";
import { JobBrowseItem } from "@/library/types/jobs/JobBrowse";
import { defineComponent } from "vue";

const context = getRundeckContext();
const eventBus = context.eventBus;
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
        handleClick(event) {
            if (event.target == this.$refs.itemDiv) {
                //only emit if the click was on the item div to avoid case when clicking on inputs/buttons
                eventBus.emit(
                    `browser-job-item-click:${this.job.id}`,
                    this.job
                );
            }
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