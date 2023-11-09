<template>
    <span v-if="scheduleData && scheduleData.hasSchedule">
        <i
            class="glyphicon glyphicon-time text-success"
            :title="JSON.stringify(scheduleData)"
        ></i>

      <span class="text-secondary" :title="'on server X'">
<!--      <g:relativeDate elapsed="${nextExecution}" untilClass="timeuntil text-success"/>-->
        <span v-if="scheduleData && scheduleData.nextExecution">
          {{ scheduleData['nextExecution'] }}
        </span>
      </span>
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
