<template>
    <span v-if="scheduleData && scheduleData.hasSchedule">
        <i
            class="glyphicon glyphicon-time"
            :title="JSON.stringify(scheduleData)"
        ></i>
    </span>
</template>

<script lang="ts">
import { JobBrowseItem, JobBrowseMeta } from "@/library/types/jobs/JobBrowse";
import { defineComponent } from "vue";


export default defineComponent({
    name: "JobScheduleInfo",
    props: {
        itemData: {
            type: Object,
            default: () => {},
        },
    },
    computed: {
        scheduleData(): JobBrowseMeta | undefined {
            return this.itemData?.meta;
        },
    },
});
</script>

<style scoped lang="scss"></style>
